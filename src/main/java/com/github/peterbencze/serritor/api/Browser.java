/*
 * Copyright 2019 Peter Bencze.
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

package com.github.peterbencze.serritor.api;

/**
 * Supported browsers that can be used for crawling.
 */
public enum Browser {

    HTML_UNIT("HtmlUnit"),
    CHROME("Chrome"),
    FIREFOX("Firefox"),
    REMOTE("Remote");

    private final String textualRepresentation;

    Browser(final String textualRepresentation) {
        this.textualRepresentation = textualRepresentation;
    }

    @Override
    public String toString() {
        return textualRepresentation;
    }
}
