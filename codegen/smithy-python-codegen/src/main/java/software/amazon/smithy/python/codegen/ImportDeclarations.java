/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.python.codegen;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Internal class used for aggregating imports of a file.
 */
@SmithyInternalApi
final class ImportDeclarations {

    private final Map<String, Map<String, String>> stdlibImports = new TreeMap<>();
    private final Map<String, Map<String, String>> externalImports = new TreeMap<>();
    private final Map<String, Map<String, String>> localImports = new TreeMap<>();

    ImportDeclarations addImport(String namespace, String name) {
        return addImport(namespace, name, name);
    }

    ImportDeclarations addImport(String namespace, String name, String alias) {
        if (namespace.startsWith(".")) {
            return addImportToMap(namespace, name, alias, localImports);
        }
        return addImportToMap(namespace, name, alias, externalImports);
    }

    ImportDeclarations addStdlibImport(String namespace, String name) {
        return addStdlibImport(namespace, name, name);
    }

    ImportDeclarations addStdlibImport(String namespace, String name, String alias) {
        return addImportToMap(namespace, name, alias, stdlibImports);
    }

    private ImportDeclarations addImportToMap(
            String namespace,
            String name,
            String alias,
            Map<String, Map<String, String>> importMap
    ) {
        if (name.equals("*")) {
            throw new CodegenException("Wildcard imports are forbidden.");
        }
        Map<String, String> namespaceImports = importMap.computeIfAbsent(namespace, ns -> new TreeMap<>());
        namespaceImports.put(name, alias);
        return this;
    }

    @Override
    public String toString() {
        if (externalImports.isEmpty() && stdlibImports.isEmpty() && localImports.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        if (!stdlibImports.isEmpty()) {
            formatImportList(builder, stdlibImports);
        }
        if (!externalImports.isEmpty()) {
            formatImportList(builder, externalImports);
        }
        if (!localImports.isEmpty()) {
            formatImportList(builder, localImports);
        }
        builder.append("\n");
        return builder.toString();
    }

    private void formatImportList(StringBuilder builder, Map<String, Map<String, String>> importMap) {
        for (Map.Entry<String, Map<String, String>> namespaceEntry: importMap.entrySet()) {
            String namespaceImport = formatSingleLineImport(namespaceEntry.getKey(), namespaceEntry.getValue());
            if (namespaceImport.length() > CodegenUtils.MAX_PREFERRED_LINE_LENGTH) {
                namespaceImport = formatMultiLineImport(namespaceEntry.getKey(), namespaceEntry.getValue());
            }
            builder.append(namespaceImport);
        }
        builder.append("\n");
    }

    private String formatSingleLineImport(String namespace, Map<String, String> names) {
        StringBuilder builder = new StringBuilder("from ").append(namespace).append(" import");
        for (Iterator<Map.Entry<String, String>> iter = names.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, String> entry = iter.next();
            builder.append(" ").append(entry.getKey());
            if (!entry.getKey().equals(entry.getValue())) {
                builder.append(" as ").append(entry.getValue());
            }
            if (iter.hasNext()) {
                builder.append(",");
            }
        }
        builder.append("\n");
        return builder.toString();
    }

    private String formatMultiLineImport(String namespace, Map<String, String> names) {
        StringBuilder builder = new StringBuilder("from ").append(namespace).append(" import (\n");
        for (Map.Entry<String, String> entry : names.entrySet()) {
            builder.append("    ").append(entry.getKey());
            if (!entry.getKey().equals(entry.getValue())) {
                builder.append(" as ").append(entry.getValue());
            }
            builder.append(",\n");
        }
        builder.append(")\n");
        return builder.toString();
    }
}