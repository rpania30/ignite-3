/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

namespace Apache.Ignite.Internal.Generators
{
    using System;
    using System.Collections.Generic;
    using System.Collections.Immutable;
    using System.IO;
    using System.Linq;
    using System.Reflection;
    using System.Text;
    using System.Text.RegularExpressions;
    using Microsoft.CodeAnalysis;

    /// <summary>
    /// Generates exception classes from Java exceptions.
    /// </summary>
    [Generator]
    public sealed class ExceptionsGenerator : JavaToCsharpGeneratorBase
    {
        /// <inheritdoc/>
        protected override IEnumerable<(string Name, string Code)> ExecuteInternal(GeneratorExecutionContext context)
        {
            var javaModulesDirectory = context.GetJavaModulesDirectory();

            var javaExceptionsWithParents = Directory.EnumerateFiles(
                    javaModulesDirectory,
                    "*Exception.java",
                    SearchOption.AllDirectories)
                .Where(x => !x.Contains("internal"))
                .Select(File.ReadAllText)
                .Select(x => (
                    Class: Regex.Match(x, @"public class (\w+) extends (\w+)"),
                    Source: x))
                .Where(x => x.Class.Success)
                .Where(x => !x.Class.Value.Contains("RaftException")) // Ignore duplicate RaftException.
                .Where(x => !x.Class.Value.Contains("IgniteClient")) // Skip Java client exceptions.
                .ToDictionary(x => x.Class.Groups[1].Value, x => (Parent: x.Class.Groups[2].Value, x.Source));

            var existingExceptions = context.Compilation.SyntaxTrees
                .Where(x => x.FilePath.Contains("Exception"))
                .Select(x => Path.GetFileNameWithoutExtension(x.FilePath))
                .ToImmutableHashSet();

            var javaExceptions = javaExceptionsWithParents
                .Where(x => !existingExceptions.Contains(x.Key))
                .Where(x => IsIgniteException(x.Key))
                .ToList();

            if (javaExceptionsWithParents.Count == 0 || javaExceptions.Count == 0)
            {
                throw new Exception($"Failed to detect Java exception classes in {javaModulesDirectory}.");
            }

            var template = GetExceptionClassTemplate();

            var classMap = new List<(string JavaClass, string DotNetClass)>();

            foreach (var javaException in javaExceptions)
            {
                var className = javaException.Key;

                var (javaPackage, dotNetNamespace) = GetPackageAndNamespace(className, javaException.Value.Source);

                var src = template
                    .Replace("IgniteTemplateException", className)
                    .Replace("XMLDOC", GetXmlDoc(className, javaException.Value.Source))
                    .Replace("NAMESPACE", dotNetNamespace);

                yield return (className + ".g.cs", src);

                classMap.Add((javaPackage + "." + className, dotNetNamespace + "." + className));
            }

            yield return EmitClassMap(classMap);

            bool IsIgniteException(string? ex) =>
                ex != null &&
                (ex == "IgniteException" ||
                 IsIgniteException(javaExceptionsWithParents.TryGetValue(ex, out var parent) ? parent.Parent : null));
        }

        private static (string Name, string Code) EmitClassMap(List<(string JavaClass, string DotNetClass)> classMap)
        {
            var sb = new StringBuilder();

            sb.AppendLine("// <auto-generated/>");
            sb.AppendLine("#nullable enable");
            sb.AppendLine("namespace Apache.Ignite.Internal");
            sb.AppendLine("{");
            sb.AppendLine("    using System;");
            sb.AppendLine();
            sb.AppendLine("    internal static class ExceptionMapper");
            sb.AppendLine("    {");
            sb.AppendLine("        public static IgniteException GetException(Guid traceId, int code, string javaClass, string? message, string? javaStackTrace) =>");
            sb.AppendLine("            javaClass switch");
            sb.AppendLine("            {");

            foreach (var (javaClass, dotNetClass) in classMap)
            {
                sb.AppendLine($"                \"{javaClass}\" => new {dotNetClass}(traceId, code, message, new IgniteException(traceId, code, javaStackTrace ?? javaClass)),");
            }

            sb.AppendLine("                _ => new IgniteException(traceId, code, message, new IgniteException(traceId, code, javaStackTrace ?? javaClass))");
            sb.AppendLine("            };");
            sb.AppendLine("    }");
            sb.AppendLine("}");

            return ("ExceptionMapper.g.cs", sb.ToString());
        }

        private static string GetXmlDoc(string javaClassName, string javaSource)
        {
            var javaDocMatch = Regex.Match(javaSource, @"/\*\*\s*\*?\s*(.*?)\s*\*/\s+public class", RegexOptions.Singleline);

            if (!javaDocMatch.Success)
            {
                throw new Exception($"Failed to parse Java package name from '{javaClassName}.java'");
            }

            var xmlDoc = javaDocMatch.Groups[1].Value
                .Replace(Environment.NewLine, " ")
                .Replace('\n', ' ')
                .Replace(" * ", " ");

            return xmlDoc;
        }

        private static (string JavaPackage, string DotNetNamespace) GetPackageAndNamespace(string javaClassName, string javaSource)
        {
            var javaPackageMatch = Regex.Match(javaSource, @"package org\.apache(\.[a-z.]+);");

            if (!javaPackageMatch.Success)
            {
                throw new Exception($"Failed to parse Java package name from '{javaClassName}.java'");
            }

            var javaPackage = javaPackageMatch.Groups[1].Value;

            var ns = Regex.Replace(javaPackage, @"(\.[a-z])", x => x.Groups[1].Value.ToUpperInvariant())
                .Replace(".Lang", string.Empty);

            return ("org.apache" + javaPackage, "Apache" + ns);
        }

        private static string GetExceptionClassTemplate()
        {
            using var stream = Assembly.GetExecutingAssembly().GetManifestResourceStream(
                "Apache.Ignite.Internal.Generators.ExceptionTemplate.cs");

            using var reader = new StreamReader(stream!);

            return reader.ReadToEnd();
        }
    }
}
