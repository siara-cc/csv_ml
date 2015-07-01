package cc.siara.csv_ml;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class MultiLevelCSVSchema {
	private Hashtable<String, Node> name_path_node_map = new Hashtable<String, Node>();
	private Hashtable<String, Node> seq_path_node_map = new Hashtable<String, Node>();
	public static class Node {
        String name;
        String path;
        List<Column> columns = new LinkedList<Column>();
        String seq_path;
        List<String> zero_children = new LinkedList<String>();
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getPath() {
			return path;
		}
		public void setPath(String path) {
			this.path = path;
		}
		public List<Column> getColumns() {
			return columns;
		}
		public void addColumn(Column column) {
			this.columns.add(column);
		}
		public String getSeqPath() {
			return seq_path;
		}
		public void setSeqPath(String seq_path) {
			this.seq_path = seq_path;
		}
		public List<String> getZeroChildren() {
			return zero_children;
		}
		public void addZeroChild(String zero_child) {
			this.zero_children.add(zero_child);
		}
	}
	public static class Column {
		String ns;
		String name;
		String alias;
		String len;
		String type;
		String col_default;
		List<String> values;
		public String getNs() {
			return ns;
		}
		public void setNs(String ns) {
			this.ns = ns;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getAlias() {
			return alias;
		}
		public void setAlias(String alias) {
			this.alias = alias;
		}
		public String getLen() {
			return len;
		}
		public void setLen(String len) {
			this.len = len;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getColDefault() {
			return col_default;
		}
		public void setColDefault(String col_default) {
			this.col_default = col_default;
		}
		public List<String> getValues() {
			return values;
		}
		public void setValues(List<String> values) {
			this.values = values;
		}
	}
	public Hashtable<String, Node> getNamePathNodeMap() {
		return name_path_node_map;
	}
	public void addNamePathNode(String path, Node node) {
		this.name_path_node_map.put(path, node);
	}
	public Hashtable<String, Node> getSeqPathNodeMap() {
		return seq_path_node_map;
	}
	public void setSeqPathNodeMap(String seq_path, Node node) {
		this.seq_path_node_map.put(seq_path, node);
	}
	public Node getNodeBySeqPath(String seq_path) {
		return seq_path_node_map.get(seq_path);
	}
	public Node getNodeByNamePath(String name_path) {
		return name_path_node_map.get(name_path);
	}
}
