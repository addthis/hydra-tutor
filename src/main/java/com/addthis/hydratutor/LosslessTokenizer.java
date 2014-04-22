/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.hydratutor;

import java.util.ArrayList;
import java.util.List;

import com.addthis.basis.util.Strings;

import com.addthis.codec.Codec;

public class LosslessTokenizer implements Codec.Codable {

    @Codec.Set(codable = true)
    private String separator = ",";
    @Codec.Set(codable = true)
    private String group[];
    @Codec.Set(codable = true)
    private boolean pack;
    @Codec.Set(codable = true)
    private String escape = "\\";

    private int maxColCount;
    private String quoteOpen;
    private String quoteClose;
    private boolean isInitialized;
    private char esc = '\\';

    public LosslessTokenizer() {
    }

    public LosslessTokenizer(String separator, String[] group, boolean pack) {
        this.separator = separator;
        this.group = group;
        this.pack = pack;
        initialize();
    }


    public LosslessTokenizer setSeparator(String sep) {
        this.separator = sep;
        return this;
    }

    public LosslessTokenizer setGrouping(String group[]) {
        this.group = group;
        return this;
    }

    public LosslessTokenizer setPacking(boolean pack) {
        this.pack = pack;
        return this;
    }

    public LosslessTokenizer initialize() {
        if (separator == null) {
            throw new RuntimeException("separator not set");
        }
        if (group != null) {
            StringBuilder open = new StringBuilder();
            StringBuilder close = new StringBuilder();
            for (String q : group) {
                if (q.length() == 1) {
                    open.append(q.charAt(0));
                    close.append(q.charAt(0));
                } else if (q.length() == 2) {
                    open.append(q.charAt(0));
                    close.append(q.charAt(1));
                } else {
                    throw new RuntimeException("invalid match " + q);
                }
            }
            quoteOpen = open.toString();
            quoteClose = close.toString();
        }
        esc = escape.charAt(0);
        isInitialized = true;
        return this;
    }

    /**
     * @return the separator string (used to split fields)
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * @return the grouping strings (used to "quote" text that might contain the
     * separator)
     */
    public String[] getGrouping() {
        return group;
    }

    /**
     * @return will all fields be packed into one?
     */
    public boolean isPacked() {
        return pack;
    }

    /**
     * @return filtered line
     */
    public String filterLine(String line) {
        return line;
    }

    /**
     * @return filtered value
     */
    public String filterValue(String value) {
        return value;
    }

    /**
     * @return splits the supplied string and returns the parts
     */
    public List<String> tokenize(String line) {
        if (!isInitialized) {
            initialize();
        }

        if (line == null || Strings.isEmpty(line.trim())) {
            return null;
        }

        line = filterLine(line);

        if (line == null) {
            return null;
        }

        List<String> ret = new ArrayList<String>(maxColCount);

        int inGroup = -1;
        boolean isEscaped = false;
        boolean isSep = false;
        int pos = 0;
        StringBuilder sb = new StringBuilder();

        while (true) {
            boolean eol = pos == line.length();
            if ((isSep && inGroup < 0) || eol) {
                if (sb.length() > 0 || !pack) {
                    ret.add(filterValue(sb.toString()));
                    sb = new StringBuilder();
                    if (isSep && eol && !pack) {
                        ret.add(filterValue(sb.toString()));
                    }
                }
            }
            if (eol) {
                break;
            }
            char ch = line.charAt(pos++);
            if (isEscaped) {
                sb.append(ch);
                isEscaped = false;
                continue;
            }
            if (ch == esc) {
                isEscaped = true;
                isSep = false;
                continue;
            }
            // check for group close
            if (inGroup >= 0) {
                if (ch == quoteClose.charAt(inGroup)) {
                    inGroup = -1;
                }
                sb.append(ch);
                continue;
            } else if (group != null) {
                // check for group open
                int qspos = quoteOpen.indexOf(ch);
                if (qspos >= 0) {
                    isSep = false;
                    inGroup = qspos;
                    sb.append(ch);
                    continue;
                }
            }
            // check for separator
            if (isSep = (separator.indexOf(ch) >= 0)) {
                continue;
            }
            sb.append(ch);
        }

        maxColCount = Math.max(maxColCount, ret.size());

        return ret;
    }
}
