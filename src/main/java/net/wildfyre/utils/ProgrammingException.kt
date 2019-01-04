/*
 * Copyright 2018 Wildfyre.net
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

package net.wildfyre.utils

/**
 * This exception is used to signify impossible errors. If this error ever occurs,
 * please report to the WildFyre team as something has gone wrong on our side.
 */
class ProgrammingException(details: String)
    : RuntimeException("This should not ever happen. Please contact the WildFyre team.\n$details")
