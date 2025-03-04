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

package org.apache.ignite.internal.rest.problem;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.apache.ignite.internal.rest.api.Problem;
import org.apache.ignite.internal.rest.api.Problem.ProblemBuilder;

/**
 * Creates {@link HttpResponse} from {@link Problem}.
 */
public final class HttpProblemResponse {
    private HttpProblemResponse() {
    }

    /**
     * Create {@link HttpResponse} from {@link Problem}.
     */
    public static HttpResponse<Problem> from(Problem problem) {
        return HttpResponse.status(HttpStatus.valueOf(problem.status())).body(problem);
    }

    /**
     * Create {@link HttpResponse} from {@link ProblemBuilder}.
     */
    public static HttpResponse<? extends Problem> from(ProblemBuilder problemBuilder) {
        return from(problemBuilder.build());
    }
}
