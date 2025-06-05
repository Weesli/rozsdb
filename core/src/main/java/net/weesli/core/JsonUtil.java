package net.weesli.core;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonReader;
import com.dslplatform.json.runtime.Settings;
import net.weesli.core.model.DataMeta;
import net.weesli.services.json.JsonBase;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtil {
    public static final DslJson<Object> dslJson;

    static {
        dslJson = new DslJson<>(Settings.withRuntime());
    }
}
