/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.xml;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Configuration information for a context file.
 *
 * @param path The path at which to store the context file.
 * @param configuredPath The path string as configured in the domain property file.
 * @param schema The schema with which to validate the file.
 * @param hasLabel Whether the context file has a specific label defined.
 * @param hasPlaceholder Whether the context file has a specific placeholder text defined.
 * @param combinationPlaceholder An optional identifier used as the placeholder for the context file in a combination template file.
 * @param index The sequential index of the context file's configuration entry.
 * @param defaultConfig Whether the context file is a default one (as opposed to a per-validation type one).
 */
public record ContextFileConfig(Path path, String configuredPath, Optional<Path> schema, boolean hasLabel, boolean hasPlaceholder, Optional<String> combinationPlaceholder, int index, boolean defaultConfig) {
}
