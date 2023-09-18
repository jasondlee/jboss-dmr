/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.dmr;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Validation {
    private Validation() {}

    public static final ModelNode SCHEMA_DEFINITION;

    public static void validate(ModelNode schema, String rootType) throws IllegalArgumentException {

    }

    static {
        ModelNode schema = new ModelNode();
        ModelNode schemaRoot = schema.get("schemaRoot");
        schemaRoot.get("description").set("The root of a schema.");
        schemaRoot.get("type").set(ModelType.OBJECT);
        schemaRoot.get("propertyType").set("map");
        schemaRoot.get("property", "type").set(ModelType.OBJECT);
        ModelNode typeSpec = schema.get("typeSpecification");
        typeSpec.get("description").set("A description of a specific node in a model graph.");
        typeSpec.get("type").set(ModelType.OBJECT);
        typeSpec.get("propertyType").set("constrained");
        typeSpec.get("property", "description", "type").set(ModelType.STRING);
        typeSpec.get("property", "description", "recommended").set(true);
        typeSpec.get("property", "description", "description").set("The description of the model element type.");
        typeSpec.get("property", "type", "type").set(ModelType.TYPE);
        typeSpec.get("property", "type", "required").set(true);
        typeSpec.get("property", "type", "description").set("The type of the model element type.");
        typeSpec.get("property", "property", "type").set("typeSpecification");
        typeSpec.get("property", "property", "required").set(true);
        typeSpec.get("property", "property", "description").set("The parameters allowed within this model element type, if it is an OBJECT.");
        typeSpec.get("property", "required", "type").set(ModelType.BOOLEAN);
        typeSpec.get("property", "required", "default").set(false);
        typeSpec.get("property", "required", "description").set("Specifies whether a parameter is required to be given.");
        typeSpec.get("property", "recommended", "type").set(ModelType.BOOLEAN);
        typeSpec.get("property", "recommended", "default").set(false);
        typeSpec.get("property", "recommended", "description").set("Specifies whether a parameter is recommended to be given.");
        typeSpec.get("property", "default", "description").set("The default value of this element type.");
        schema.protect();
        SCHEMA_DEFINITION = schema;
    }

}
