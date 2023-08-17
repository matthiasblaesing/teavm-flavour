/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.flavour.rest;

/**
 *
 * @author Alexey Andreev
 */
public class JsonParseException extends RESTException {
    private static final long serialVersionUID = -7449483893122040982L;
    private String jsonString;

    public JsonParseException(String jsonString) {
        super("Error parsing JSON: " + jsonString);
        this.jsonString = jsonString;
    }

    public String getJsonString() {
        return jsonString;
    }
}
