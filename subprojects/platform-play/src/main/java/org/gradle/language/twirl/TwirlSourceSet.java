/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.language.twirl;

import org.gradle.api.Incubating;
import org.gradle.language.base.LanguageSourceSet;

import java.util.List;

/**
 * Represents a source set containing twirl templates
 */
@Incubating
public interface TwirlSourceSet extends LanguageSourceSet {
    /**
     * The default imports that should be added to generated source files
     */
    TwirlImports getDefaultImports();

    /**
     * Sets the default imports that should be added to generated source files to the given language
     */
    void setDefaultImports(TwirlImports defaultImports);


    /**
     * Returns the custom template formats configured for this source set.
     */
    List<TwirlTemplateFormat> getUserTemplateFormats();

    /**
     * Sets the custom template formats for this source set.
     */
    void setUserTemplateFormats(List<TwirlTemplateFormat> userTemplateFormats);

    /**
     * Adds a custom template format.
     *
     * @param extension file extension this template applies to (e.g., {@code html}).
     * @param templateType fully-qualified type for this template format.
     * @param imports additional imports to add for the custom template format.
     */
    void addUserTemplateFormat(final String extension, String templateType, String... imports);
}
