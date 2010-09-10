/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Tim Azzopardi <tim@tigerfive.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.environment;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;

import org.jruby.Ruby;
import org.jruby.ext.posix.util.Platform;
import org.jruby.util.ByteList;

public class OSEnvironment {
    /**
     * Returns the environment as a hash of Ruby strings.
     *
     * @param runtime
     */
    public Map getEnvironmentVariableMap(Ruby runtime) {
        Map envs = null;

        if (runtime.getInstanceConfig().getEnvironment() != null) {
            return getAsMapOfRubyStrings(runtime, runtime.getInstanceConfig().getEnvironment().entrySet());
        }

        // fall back on empty env when security disallows environment var access (like in an applet)
        if (Ruby.isSecurityRestricted())
            envs = new HashMap();
        else {
            Map variables = System.getenv();
            envs = getAsMapOfRubyStrings(runtime,  variables.entrySet());
        }

        return envs;

    }

    /**
    * Returns java system properties as a Map<RubyString,RubyString>.
     * @param runtime
     * @return the java system properties as a Map<RubyString,RubyString>.
     */
    public Map getSystemPropertiesMap(Ruby runtime) {
        if (Ruby.isSecurityRestricted())
           return new HashMap();
       else
           return getAsMapOfRubyStrings(runtime, System.getProperties().entrySet());
    }
    
	private static Map getAsMapOfRubyStrings(Ruby runtime, Set<Map.Entry<Object, Object>> entrySet) {
		Map envs = new HashMap();
        Encoding encoding = EncodingDB.getEncodings().get(Charset.defaultCharset().name().getBytes()).getEncoding();
        
        // On Windows, entrySet doesn't have corresponding keys for these
        if (Platform.IS_WINDOWS) {
            envs.put(runtime.newString("HOME"), runtime.newString(System.getProperty("user.home")));
            envs.put(runtime.newString("USER"), runtime.newString(System.getProperty("user.name")));
        }

        for (Map.Entry<Object, Object> entry : entrySet) {
            String value = (String)entry.getValue();
            String key = (String)entry.getKey();

            ByteList keyBytes = new ByteList(key.getBytes(), encoding);
            ByteList valueBytes = new ByteList(value.getBytes(), encoding);

            envs.put(runtime.newString(keyBytes), runtime.newString(valueBytes));
		}
        
		return envs;
	}
}
