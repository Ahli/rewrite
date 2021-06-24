/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.config;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.openrewrite.Recipe;
import org.openrewrite.internal.RecipeIntrospectionUtils;
import org.openrewrite.style.NamedStyles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class ClasspathScanningLoader implements ResourceLoader {
    private static final Logger logger = LoggerFactory.getLogger(ClasspathScanningLoader.class);

    private final List<Recipe> recipes = new ArrayList<>();
    private final List<NamedStyles> styles = new ArrayList<>();

    private final List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();
    private final List<CategoryDescriptor> categoryDescriptors = new ArrayList<>();

    /**
     * Construct a ClasspathScanningLoader scans the runtime classpath of the current java process for recipes
     *
     * @param properties Yaml placeholder properties
     * @param acceptPackages Limit scan to specified packages
     */
    public ClasspathScanningLoader(Properties properties, String[] acceptPackages) {
        scanYaml(new ClassGraph().acceptPaths("META-INF/rewrite"), properties, null);
        scanClasses(new ClassGraph().acceptPackages(acceptPackages), getClass().getClassLoader());
    }

    public ClasspathScanningLoader(Path jar, Properties properties, ClassLoader classLoader) {
        String jarName = jar.toFile().getName();

        scanYaml(new ClassGraph()
                .acceptJars(jarName)
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classLoader)
                .acceptPaths("META-INF/rewrite"), properties, classLoader);

        scanClasses(new ClassGraph()
                .acceptJars(jarName)
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classLoader), classLoader);
    }

    private void scanYaml(ClassGraph classGraph, Properties properties, @Nullable ClassLoader classLoader) {
        try (ScanResult scanResult = classGraph.enableMemoryMapping().scan()) {
            List<YamlResourceLoader> yamlResourceLoaders = new ArrayList<>();

            scanResult.getResourcesWithExtension("yml").forEachInputStreamIgnoringIOException((res, input) -> {
                yamlResourceLoaders.add(new YamlResourceLoader(input, res.getURI(), properties, classLoader));
            });
            // Extract in two passes so that the full list of recipes from all sources are known when computing recipe descriptors
            // Otherwise recipes which include recipes from other sources in their recipeList will have incomplete descriptors
            for(YamlResourceLoader resourceLoader : yamlResourceLoaders) {
                recipes.addAll(resourceLoader.listRecipes());
                categoryDescriptors.addAll(resourceLoader.listCategoryDescriptors());
                styles.addAll(resourceLoader.listStyles());
            }
            for(YamlResourceLoader resourceLoader : yamlResourceLoaders) {
                recipeDescriptors.addAll(resourceLoader.listRecipeDescriptors(recipes));
            }
        }
    }

    private void scanClasses(ClassGraph classGraph, ClassLoader classLoader) {
        try (ScanResult result = classGraph
                .ignoreClassVisibility()
                .scan()) {
            for (ClassInfo classInfo : result.getSubclasses(Recipe.class.getName())) {
                Class<?> recipeClass = classInfo.loadClass();
                if (recipeClass.equals(DeclarativeRecipe.class) || recipeClass.getEnclosingClass() != null) {
                    continue;
                }
                try {
                    recipeDescriptors.add(RecipeIntrospectionUtils.recipeDescriptorFromRecipeClass(recipeClass));
                    Constructor<?> constructor = RecipeIntrospectionUtils.getZeroArgsConstructor(recipeClass);

                    if (constructor != null) {
                        constructor.setAccessible(true);
                        recipes.add((Recipe) constructor.newInstance());
                    }
                } catch (Exception e) {
                    logger.warn("Unable to configure {}", recipeClass.getName(), e);
                }
            }
            for (ClassInfo classInfo : result.getSubclasses(NamedStyles.class.getName())) {
                Class<?> styleClass = classInfo.loadClass();
                try {
                    Constructor<?> constructor = RecipeIntrospectionUtils.getZeroArgsConstructor(styleClass);
                    if(constructor != null) {
                        constructor.setAccessible(true);
                        styles.add((NamedStyles) constructor.newInstance());
                    }
                } catch (Exception e) {
                    logger.warn("Unable to configure {}", styleClass.getName(), e);
                }
            }
        }
    }

    @Override
    public Collection<Recipe> listRecipes() {
        return recipes;
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        return recipeDescriptors;
    }

    @Override
    public Collection<CategoryDescriptor> listCategoryDescriptors() {
        return categoryDescriptors;
    }

    @Override
    public Collection<NamedStyles> listStyles() {
        return styles;
    }
}
