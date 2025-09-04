package com.toxisafe.sync;

import java.util.Map;

public interface SyncEmitter {
    void emitInsert(String tabla, String idRegistro, Map<String,Object> nuevos);
    void emitUpdate(String tabla, String idRegistro, Map<String,Object> antiguos, Map<String,Object> nuevos);
    void emitDelete(String tabla, String idRegistro, Map<String,Object> antiguos);
}
