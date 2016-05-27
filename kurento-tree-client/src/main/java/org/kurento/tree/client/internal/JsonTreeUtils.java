/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.tree.client.internal;

import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.JsonRpcException;
import org.kurento.jsonrpc.message.Request;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class JsonTreeUtils {

  public static <T> T getRequestParam(Request<JsonObject> request, String paramName,
      Class<T> type) {
    return getRequestParam(request, paramName, type, false);
  }

  public static <T> T getRequestParam(Request<JsonObject> request, String paramName, Class<T> type,
      boolean allowNull) {

    JsonObject params = request.getParams();
    if (params == null) {
      if (!allowNull) {
        throw new JsonRpcErrorException(1, "Invalid request lacking parameter '" + paramName + "'");
      } else {
        return null;
      }
    }
    return getConverted(params.get(paramName), paramName, type, allowNull);
  }

  public static <T> T getResponseProperty(JsonElement result, String property, Class<T> type) {

    if (!(result instanceof JsonObject)) {
      throw new JsonRpcException(
          "Invalid response format. The response '" + result + "' should be a Json object");
    }
    return getConverted(((JsonObject) result).get(property), property, type, false);
  }

  @SuppressWarnings("unchecked")
  private static <T> T getConverted(JsonElement paramValue, String property, Class<T> type,
      boolean allowNull) {
    if (paramValue == null || paramValue instanceof JsonNull) {
      if (allowNull) {
        return null;
      } else {
        throw new JsonRpcErrorException(1, "Invalid method lacking parameter '" + property + "'");
      }
    }

    if (type == String.class) {
      if (paramValue.isJsonPrimitive()) {
        return (T) paramValue.getAsString();
      }
    }

    if (type == Integer.class) {
      if (paramValue.isJsonPrimitive()) {
        return (T) Integer.valueOf(paramValue.getAsInt());
      }
    }

    throw new JsonRpcErrorException(2,
        "Param '" + property + "' with value '" + paramValue + "' is not a " + type.getName());
  }
}
