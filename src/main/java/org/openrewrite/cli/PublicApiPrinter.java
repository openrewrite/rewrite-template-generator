/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.cli;

import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.util.TraceSignatureVisitor;
import org.openrewrite.internal.lang.Nullable;

import java.util.*;

import static java.util.stream.Collectors.toList;

@SuppressWarnings({"DynamicRegexReplaceableByCompiledPattern", "UseOfSystemOutOrSystemErr", "StringRepeatCanBeUsed", "StringBufferField"})
public class PublicApiPrinter extends ClassVisitor {
     private static final String tab = "  ";

     private int depth;
     private StringBuilder stringBuilder;

     private String className;
     private String packageName;

     private int access;
     private boolean lastVisitedWasEnum;

     private Set<String> printedMethodDescriptors;

     private final Map<String, PublicApi> publicApisByName = new TreeMap<>();

     @SuppressWarnings("UseOfSystemOutOrSystemErr")
     private final class PublicApi {
          private final String name;
          private final StringBuilder contents;

          private PublicApi(String name, StringBuilder contents) {
               this.name = name;
               this.contents = contents;
          }

          void print() {
               System.out.print(contents.toString());
               publicApisByName.values().stream()
                       .filter(pa -> name.equals(pa.getParent()))
                       .forEach(PublicApi::print);
               for (int i = 0; i < getDepth(); i++) {
                    System.out.print(tab);
               }
               System.out.print("}\n");
          }

          @Nullable
          public String getParent() {
               return getParent(name);
          }

          private String getParent(String name) {
               if (!name.contains("$")) {
                    return null;
               }
               return name.substring(0, name.lastIndexOf('$'));
          }

          public int getDepth() {
               int depth = 0;
               String parent = name;
               while ((parent = getParent(parent)) != null) {
                    depth++;
               }
               return depth;
          }
     }

     public PublicApiPrinter() {
          super(Opcodes.ASM9);
     }

     @Override
     public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
          printedMethodDescriptors = new HashSet<>();
          lastVisitedWasEnum = false;
          // this is preventing abstract classes from being printed
          //        if ((access & Opcodes.ACC_PUBLIC) == 0) {
          //            return;
          //        }
          this.stringBuilder = new StringBuilder();

          PublicApi api = new PublicApi(name, stringBuilder);
          this.depth = api.getDepth();
          publicApisByName.put(name, api);

          this.access = access;
          this.packageName = Type.getObjectType(name.substring(0, name.lastIndexOf('/'))).getClassName();

          if (depth == 0) {
               stringBuilder.append("package ").append(packageName).append(";\n");
               stringBuilder.append("import java.io.*;\n\n");
          }

          appendAccess(access & ~(Opcodes.ACC_SUPER | Opcodes.ACC_MODULE));
          if ((access & Opcodes.ACC_ANNOTATION) != 0) {
               stringBuilder.append("@interface ");
          } else if ((access & Opcodes.ACC_INTERFACE) != 0) {
               stringBuilder.append("interface ");
          } else if ((access & Opcodes.ACC_ENUM) == 0) {
               stringBuilder.append("class ");
          }

          this.className = name.substring(name.lastIndexOf('/') + 1).replaceFirst("^.*\\$", "");
          stringBuilder.append(this.className);

          if (superName != null && !"java/lang/Object".equals(superName) && !"java/lang/Enum".equals(superName)) {
               stringBuilder.append(" extends ");
               appendType(Type.getObjectType(superName).getDescriptor());
          }
          if (interfaces != null && interfaces.length > 0) {
               List<String> printableInterfaces = Arrays.stream(interfaces)
                       .filter(i -> !"java/lang/annotation/Annotation".equals(i))
                       .collect(toList());
               if (!printableInterfaces.isEmpty()) {
                    stringBuilder.append(((access & Opcodes.ACC_INTERFACE) != 0) ? " extends " : " implements ");
                    for (int i = 0; i < printableInterfaces.size(); ++i) {
                         appendType(Type.getObjectType(printableInterfaces.get(i)).getDescriptor());
                         if (i != printableInterfaces.size() - 1) {
                              stringBuilder.append(", ");
                         }
                    }
               }
          }
          stringBuilder.append(" {\n");
     }

     private void appendAccess(int accessFlags) {
          appendIndent();
          if ((accessFlags & Opcodes.ACC_PUBLIC) != 0) {
               stringBuilder.append("public ");
          }
          if ((accessFlags & Opcodes.ACC_PRIVATE) != 0) {
               stringBuilder.append("private ");
          }
          if ((accessFlags & Opcodes.ACC_PROTECTED) != 0) {
               stringBuilder.append("protected ");
          }
          if ((accessFlags & Opcodes.ACC_STATIC) != 0) {
               stringBuilder.append("static ");
          }
          if ((accessFlags & Opcodes.ACC_ABSTRACT) != 0 &&
                  (accessFlags & Opcodes.ACC_ENUM) == 0 &&
                  (accessFlags & Opcodes.ACC_INTERFACE) == 0) {
               stringBuilder.append("abstract ");
          }
          if ((accessFlags & Opcodes.ACC_STRICT) != 0) {
               stringBuilder.append("strictfp ");
          }
          if ((accessFlags & Opcodes.ACC_ENUM) != 0) {
               stringBuilder.append("enum ");
          }
     }

     @Override
     public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
          if (isNotPublicOuterClass() || (access & Opcodes.ACC_PUBLIC) == 0) {
               return null;
          }

          if ((access & Opcodes.ACC_ENUM) == 0) {
               if (lastVisitedWasEnum) {
                    appendIndent();
                    stringBuilder.append(tab).append(";\n");
                    lastVisitedWasEnum = false;
               }
               stringBuilder.append(tab);
               appendAccess(access);
               appendType(descriptor, signature);
               stringBuilder.append(' ');
          } else {
               appendIndent();
               stringBuilder.append(tab);
          }

          stringBuilder.append(name);

          if ((access & Opcodes.ACC_ENUM) == 0) {
               stringBuilder.append(";\n");
          } else {
               stringBuilder.append(",\n");
               lastVisitedWasEnum = true;
          }

          return null;
     }

     private void appendType(String descriptor) {
          appendType(descriptor, null);
     }

     private void appendType(String descriptor, String signature) {
          String type;
          if (signature != null) {
               TraceSignatureVisitor traceSignatureVisitor = new TraceSignatureVisitor(access);
               new SignatureReader(signature).accept(traceSignatureVisitor);
               type = traceSignatureVisitor.getDeclaration().replaceFirst("^\\sextends\\s", "");
          } else {
               type = Type.getType(descriptor).getClassName();
          }

          type = type.replaceAll("java\\.(io)\\.", "");
          if (type.contains(".") && type.substring(0, type.lastIndexOf('.')).equals(packageName)) {
               type = type.substring(type.lastIndexOf('.') + 1);
          }
          type = type.replace('$', '.');

          stringBuilder.append(type);
     }

     private String extractNameAndArguments(String methodName, String descriptor) {
          return methodName + descriptor.substring(0, descriptor.indexOf(")") + 1) ;
     }

     @Override
     public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
          String nameArgs = extractNameAndArguments(name, descriptor);
          if (printedMethodDescriptors.contains(nameArgs)) {
               return null;
          }
          printedMethodDescriptors.add(nameArgs);
          if (isNotPublicOuterClass() || (access & Opcodes.ACC_PUBLIC) == 0) {
               return null;
          }

          if ((this.access & Opcodes.ACC_ENUM) != 0 && ("values".equals(name) || "valueOf".equals(name))) {
               return null;
          }

          if (lastVisitedWasEnum) {
               appendIndent();
               stringBuilder.append(tab).append(";\n");
               lastVisitedWasEnum = false;
          }

          if ((this.access & Opcodes.ACC_INTERFACE) != 0
                  && (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC)) == 0) {
               //            stringBuilder.append("default ");
               return null;
          }

          stringBuilder.append(tab);

          if ((this.access & Opcodes.ACC_INTERFACE) != 0) {
               access &= ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT);
          }

          appendAccess(access & ~(Opcodes.ACC_VOLATILE | Opcodes.ACC_TRANSIENT));

          Type methodType = Type.getMethodType(descriptor);

          appendType(methodType.getReturnType().getDescriptor());

          stringBuilder.append(' ');
          if ("<init>".equals(name)) {
               stringBuilder.append(className);
          } else {
               stringBuilder.append(name);
          }

          stringBuilder.append('(');
          Type[] argumentTypes = methodType.getArgumentTypes();
          for (int i = 0; i < argumentTypes.length; i++) {
               Type argumentType = argumentTypes[i];
               appendType(argumentType.getDescriptor());
               stringBuilder.append(" p").append(i);
               if (i < argumentTypes.length - 1) {
                    stringBuilder.append(", ");
               }
          }
          stringBuilder.append(')');

          if (exceptions != null && exceptions.length > 0) {
               stringBuilder.append(" throws ");
               for (int i = 0; i < exceptions.length; i++) {
                    String exception = exceptions[i];
                    appendType(Type.getObjectType(exception).getDescriptor());
                    if (i > exceptions.length - 1) {
                         stringBuilder.append(' ');
                    }
               }
          }

          if (((access & Opcodes.ACC_ABSTRACT) == 0) && ((this.access & Opcodes.ACC_INTERFACE) == 0)) {
               stringBuilder.append(" {");
               if (methodType.getReturnType().getSort() != Type.VOID) {
                    stringBuilder.append(" return (");
                    appendType(methodType.getReturnType().getDescriptor());
                    stringBuilder.append(") (Object) null; ");
               }
               stringBuilder.append('}');
          }
          else {
               stringBuilder.append(";");
          }
          stringBuilder.append('\n');

          return null;
     }

     private void appendIndent() {
          for (int i = 0; i < depth; i++) {
               stringBuilder.append(tab);
          }
     }

     private boolean isNotPublicOuterClass() {
          return (access & Opcodes.ACC_PUBLIC) == 0;
     }

     public void print() {
          for (PublicApi api : publicApisByName.values()) {
               if (api.getDepth() == 0) {
                    api.print();
                    System.out.println("---");
               }
          }
     }
}
