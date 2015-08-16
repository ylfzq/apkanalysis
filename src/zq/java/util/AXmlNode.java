package zq.java.util;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.xmlpull.v1.XmlPullParser;

import android.content.res.AXmlResourceParser;
import android.util.TypedValue;


public class AXmlNode {

	public final String name;
	public final int depth;
	private Map<String, String> mAttributes = new HashMap<String, String>();
	private List<AXmlNode> mChildNodes = new ArrayList<AXmlNode>();


	public static AXmlNode createNodeTree(InputStream in) {

		AXmlNode root = new AXmlNode("root", 0);
		try {
			Stack<AXmlNode> stack = new Stack<AXmlNode>();
			stack.push(root);

			AXmlResourceParser parser = new AXmlResourceParser();
			parser.open(in);
			while (true) {
				int type = parser.next();
				if (type == XmlPullParser.END_DOCUMENT)
					break;

				switch (type) {
				case XmlPullParser.START_TAG: {
					AXmlNode parent = stack.peek();
					AXmlNode node =
						new AXmlNode(parser.getName(), parser.getDepth());
					int count = parser.getAttributeCount();
					for (int i = 0; i < count; ++i) {
						node.mAttributes.put(
							parser.getAttributeName(i),
							getAttributeValue(parser, i));
					}
					parent.mChildNodes.add(node);
					stack.push(node);
					break;
				}
				case XmlPullParser.END_TAG: {
					stack.pop();
					break;
				}
				}
			}
		} catch (Exception e) {
			root = null;
			e.printStackTrace();
		}
		return root;
	}


	public AXmlNode(String name, int depth) {

		this.name = name;
		this.depth = depth;
	}


	public String getAttribute(String attributeName) {

		return mAttributes.get(attributeName);
	}


	public String getAttribute(String[] nodePath, String attributeName) {

		AXmlNode node = getAXmlNode(nodePath, 0, nodePath.length);
		if (node == null)
			return null;
		return node.getAttribute(attributeName);
	}


	public List<String> getAttributes(String[] nodePath, String attributeName) {

		AXmlNode node = getAXmlNode(nodePath, 0, nodePath.length - 1);
		if (node == null)
			return null;
		List<AXmlNode> nodeList =
			node.getChildren(nodePath[nodePath.length - 1]);
		List<String> list = new ArrayList<String>(nodeList.size());
		for (AXmlNode n : nodeList) {
			list.add(n.getAttribute(attributeName));
		}
		return list;
	}


	public AXmlNode getChild(int i) {

		return mChildNodes.get(i);
	}


	private String[] handleNodeName(String nodeName) {

		String[] handledName = new String[2];
		int indexAt = nodeName.indexOf("@");
		if (indexAt < 0) {
			handledName[0] = nodeName;
			handledName[1] = "-1";
		} else {
			handledName[0] = nodeName.substring(0, indexAt);
			handledName[1] = nodeName.substring(indexAt + 1);
		}
		return handledName;
	}


	public AXmlNode getChild(String nodeName) {

		String[] nodeNameIndex = handleNodeName(nodeName);
		int count = Integer.parseInt(nodeNameIndex[1]);
		for (AXmlNode child : mChildNodes) {
			if (nodeNameIndex[0].equals(child.name)) {
				if (count <= 0)
					return child;
				--count;
			}
		}
		return null;
	}


	public List<AXmlNode> getChildren(String nodeName) {

		List<AXmlNode> list = new ArrayList<AXmlNode>();
		String[] nodeNameIndex = handleNodeName(nodeName);
		int count = Integer.parseInt(nodeNameIndex[1]);
		if (count >= 0) {
			AXmlNode child = getChild(nodeName);
			if (child != null)
				list.add(child);
		} else {
			for (AXmlNode n : mChildNodes) {
				if (nodeNameIndex[0].equals(n.name))
					list.add(n);
			}
		}
		return list;
	}


	public AXmlNode getAXmlNode(String[] nodePath) {

		return getAXmlNode(nodePath, 0, nodePath.length);
	}


	public AXmlNode getAXmlNode(String[] nodePath, int start, int length) {

		AXmlNode node = this;
		for (int i = start; i < length; ++i) {
			node = node.getChild(nodePath[i]);
			if (node == null)
				return null;
		}
		return node;
	}


	public List<AXmlNode> getAXmlNodes(String[] nodePath) {

		return getAXmlNodes(nodePath, 0, nodePath.length);
	}


	public List<AXmlNode> getAXmlNodes(String[] nodePath, int start, int length) {

		AXmlNode node = getAXmlNode(nodePath, start, length - 1);
		if (node == null)
			return new ArrayList<AXmlNode>();
		return node.getChildren(nodePath[length - 1]);
	}


	public List<String> getAXmlNodesDetail(String[] nodePath) {

		return getAXmlNodesDetail(nodePath, 0, nodePath.length);
	}


	public List<String> getAXmlNodesDetail(String[] nodePath, int start,
		int length) {

		List<AXmlNode> nodeList = getAXmlNodes(nodePath, start, length);
		List<String> list = new ArrayList<String>(nodeList.size());
		for (AXmlNode node : nodeList) {
			list.add(node.toString(0));
		}
		return list;
	}


	private String space(int count) {

		if (count == 0)
			return "";
		return String.format("%" + count + "s", " ");
	}


	@Override
	public String toString() {

		return toString(depth);
	}


	/* package */String toString(int depth) {

		int reduced = this.depth - depth;
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : mAttributes.entrySet()) {
			sb.append(space(depth * 4));
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(entry.getValue());
			sb.append("\n");
		}
		for (AXmlNode item : mChildNodes) {
			sb.append(space(depth * 4));
			sb.append("<");
			sb.append(item.name);
			sb.append(">\n");
			sb.append(item.toString(item.depth - reduced));
		}
		return sb.toString();
	}


	private static String getAttributeValue(AXmlResourceParser parser, int index) {

		int type = parser.getAttributeValueType(index);
		int data = parser.getAttributeValueData(index);
		if (type == TypedValue.TYPE_STRING) {
			return parser.getAttributeValue(index);
		}
		if (type == TypedValue.TYPE_ATTRIBUTE) {
			return String.format("?%s%08X", getPackage(data), data);
		}
		if (type == TypedValue.TYPE_REFERENCE) {
			return String.format("@%s%08X", getPackage(data), data);
		}
		if (type == TypedValue.TYPE_FLOAT) {
			return String.valueOf(Float.intBitsToFloat(data));
		}
		if (type == TypedValue.TYPE_INT_HEX) {
			return String.format("0x%08X", data);
		}
		if (type == TypedValue.TYPE_INT_BOOLEAN) {
			return data != 0 ? "true" : "false";
		}
		if (type == TypedValue.TYPE_DIMENSION) {
			return Float.toString(complexToFloat(data)) +
				DIMENSION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];
		}
		if (type == TypedValue.TYPE_FRACTION) {
			return Float.toString(complexToFloat(data)) +
				FRACTION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];
		}
		if (type >= TypedValue.TYPE_FIRST_COLOR_INT &&
			type <= TypedValue.TYPE_LAST_COLOR_INT) {
			return String.format("#%08X", data);
		}
		if (type >= TypedValue.TYPE_FIRST_INT &&
			type <= TypedValue.TYPE_LAST_INT) {
			return String.valueOf(data);
		}
		return String.format("<0x%X, type 0x%02X>", data, type);
	}


	private static String getPackage(int id) {

		if (id >>> 24 == 1) {
			return "android:";
		}
		return "";
	}


	// ///////////////////////////////// ILLEGAL STUFF, DONT LOOK :)
	public static float complexToFloat(int complex) {

		return (float) (complex & 0xFFFFFF00) * RADIX_MULTS[(complex >> 4) & 3];
	}


	private static final float RADIX_MULTS[] = {
		0.00390625F, 3.051758E-005F, 1.192093E-007F, 4.656613E-010F
	};
	private static final String DIMENSION_UNITS[] = {
		"px", "dip", "sp", "pt", "in", "mm", "", ""
	};
	private static final String FRACTION_UNITS[] = {
		"%", "%p", "", "", "", "", "", ""
	};
}
