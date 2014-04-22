#!/bin/sh
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Cannot determine a way to disable admin port in dropwizard so using a randomly generated password.
java -Ddw.http.adminUsername="admin" -Ddw.http.adminPassword="`< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c40`" \
   -Ddebug.threadGroup=1 -jar `ls -t target/hydra-tutor-*-exec.jar | head -n 1` server conf/hydratutor.yaml
