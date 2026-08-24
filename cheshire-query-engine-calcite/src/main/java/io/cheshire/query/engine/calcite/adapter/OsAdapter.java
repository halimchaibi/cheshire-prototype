/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.adapter;

import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import java.util.Map;
import org.apache.calcite.adapter.os.CpuInfoTableFunction;
import org.apache.calcite.adapter.os.CpuTimeTableFunction;
import org.apache.calcite.adapter.os.DuTableFunction;
import org.apache.calcite.adapter.os.FilesTableFunction;
import org.apache.calcite.adapter.os.GitCommitsTableFunction;
import org.apache.calcite.adapter.os.InterfaceAddressesTableFunction;
import org.apache.calcite.adapter.os.InterfaceDetailsTableFunction;
import org.apache.calcite.adapter.os.JavaInfoTableFunction;
import org.apache.calcite.adapter.os.JpsTableFunction;
import org.apache.calcite.adapter.os.MemoryInfoTableFunction;
import org.apache.calcite.adapter.os.MountsTableFunction;
import org.apache.calcite.adapter.os.OsVersionTableFunction;
import org.apache.calcite.adapter.os.PsTableFunction;
import org.apache.calcite.adapter.os.SystemInfoTableFunction;
import org.apache.calcite.adapter.os.VmstatTableFunction;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;

public final class OsAdapter implements SourceAdapter {

  @Override
  public Schema createSchema(
      final String name, final Map<String, Object> config, final SchemaPlus schema)
      throws QueryEngineInitializationException {
    try {
      return new StaticTableSchema(tables());
    } catch (Exception e) {
      throw new QueryEngineInitializationException(
          "Failed to create OS schema for source: " + name, e);
    }
  }

  @Override
  public String supportedType() {
    return "OS";
  }

  private Map<String, Table> tables() {
    return Map.ofEntries(
        Map.entry("system_info", SystemInfoTableFunction.eval(false)),
        Map.entry("java_info", JavaInfoTableFunction.eval(false)),
        Map.entry("os_version", OsVersionTableFunction.eval(false)),
        Map.entry("memory_info", MemoryInfoTableFunction.eval(false)),
        Map.entry("cpu_info", CpuInfoTableFunction.eval(false)),
        Map.entry("cpu_time", CpuTimeTableFunction.eval(false)),
        Map.entry("interface_addresses", InterfaceAddressesTableFunction.eval(false)),
        Map.entry("interface_details", InterfaceDetailsTableFunction.eval(false)),
        Map.entry("mounts", MountsTableFunction.eval(false)),
        Map.entry("ps", PsTableFunction.eval(false)),
        Map.entry("jps", JpsTableFunction.eval(false)),
        Map.entry("vmstat", VmstatTableFunction.eval(false)),
        Map.entry("du", DuTableFunction.eval(false)),
        Map.entry("git_commits", GitCommitsTableFunction.eval(false)),
        Map.entry("files", FilesTableFunction.eval(".")));
  }
}
