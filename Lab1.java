import java.io.*;
import java.util.*;

public class Lab1 {
    // 静态图实例，供各方法使用
    private static DirectedWeightedGraph graph = new DirectedWeightedGraph();
    private static final double DAMPING = 0.85;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String filePath;

        // 读取文件路径
        if (args.length > 0) {
            filePath = args[0];
        } else {
            System.out.print("请输入文本文件路径：");
            filePath = scanner.nextLine();
        }

        // 读文件并构建图
        List<String> words = readFileAndTokenize(filePath);
        buildGraph(words);

        // 交互菜单
        while (true) {
            System.out.println("\n请选择功能：");
            System.out.println("1. 展示有向图");
            System.out.println("2. 查询桥接词");
            System.out.println("3. 根据桥接词生成新文本");
            System.out.println("4. 计算最短路径");
            System.out.println("5. 计算PageRank");
            System.out.println("6. 随机游走");
            System.out.println("7. 退出");
            System.out.print("输入选项编号：");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    showDirectedGraph(graph);
                    break;
                case "2":
                    System.out.print("输入 word1: ");
                    String w1 = scanner.nextLine().trim().toLowerCase();
                    System.out.print("输入 word2: ");
                    String w2 = scanner.nextLine().trim().toLowerCase();
                    System.out.println(queryBridgeWords(w1, w2));
                    break;
                case "3":
                    System.out.print("输入新文本：");
                    String newText = scanner.nextLine();
                    System.out.println(generateNewText(newText));
                    break;
                case "4":
                    System.out.print("输入起点 word1: ");
                    String s1 = scanner.nextLine().trim().toLowerCase();
                    System.out.print("输入终点 word2: ");
                    String s2 = scanner.nextLine().trim().toLowerCase();
                    System.out.println(calcShortestPath(s1, s2));
                    break;
                case "5":
                    System.out.print("输入待计算PR的单词：");
                    String prWord = scanner.nextLine().trim().toLowerCase();
                    Double pr = calPageRank(prWord);
                    if (pr == null) {
                        System.out.println("单词不在图中！");
                    } else {
                        System.out.printf("PageRank(%s) = %.6f\n", prWord, pr);
                    }
                    break;
                case "6":
                    System.out.println("随机游走结果：");
                    System.out.println(randomWalk());
                    break;
                case "7":
                    System.out.println("退出。");
                    scanner.close();
                    return;
                default:
                    System.out.println("无效选项，请重新输入。");
            }
        }
    }

    // ======================== 功能函数 ========================

    // 1. 展示有向图
    public static void showDirectedGraph(DirectedWeightedGraph G) {
        System.out.println("有向图（邻接表，格式：节点 -> {目标: 权重, ...}）：");
        for (String u : G.getNodes()) {
            System.out.println(u + " -> " + G.getAdj(u));
        }
    }

    // 2. 查询桥接词
    public static String queryBridgeWords(String word1, String word2) {
        if (!graph.contains(word1) || !graph.contains(word2)) {
            return String.format("No \"%s\" or \"%s\" in the graph!", word1, word2);
        }
        Set<String> bridges = new HashSet<>();
        for (String mid : graph.getNeighbors(word1).keySet()) {
            if (graph.getNeighbors(mid).containsKey(word2)) {
                bridges.add(mid);
            }
        }
        if (bridges.isEmpty()) {
            return String.format("No bridge words from \"%s\" to \"%s\"!", word1, word2);
        }
        return String.format("The bridge words from \"%s\" to \"%s\" are: %s.",
                word1, word2, String.join(", ", bridges));
    }

    // 3. 根据桥接词生成新文本
    public static String generateNewText(String inputText) {
        // 统一清洗并分词
        String cleaned = inputText.replaceAll("[^A-Za-z]+", " ").toLowerCase().trim();
        String[] tokens = cleaned.split("\\s+");
        List<String> out = new ArrayList<>();
        Random rand = new Random();

        for (int i = 0; i < tokens.length; i++) {
            out.add(tokens[i]);
            if (i < tokens.length - 1) {
                // 找桥接词
                Set<String> bridges = new HashSet<>();
                for (String mid : graph.getNeighbors(tokens[i]).keySet()) {
                    if (graph.getNeighbors(mid).containsKey(tokens[i + 1])) {
                        bridges.add(mid);
                    }
                }
                if (!bridges.isEmpty()) {
                    // 随机选一个
                    List<String> list = new ArrayList<>(bridges);
                    out.add(list.get(rand.nextInt(list.size())));
                }
            }
        }
        return String.join(" ", out);
    }

    // 4. 计算最短路径（Dijkstra）
    public static String calcShortestPath(String word1, String word2) {
        if (!graph.contains(word1) || !graph.contains(word2)) {
            return "起点或终点不在图中！";
        }
        // Dijkstra
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        for (String u : graph.getNodes())
            dist.put(u, Integer.MAX_VALUE);
        dist.put(word1, 0);
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.add(word1);

        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (u.equals(word2))
                break;
            for (Map.Entry<String, Integer> e : graph.getNeighbors(u).entrySet()) {
                String v = e.getKey();
                int w = e.getValue();
                int nd = dist.get(u) + w;
                if (nd < dist.get(v)) {
                    dist.put(v, nd);
                    prev.put(v, u);
                    pq.remove(v);
                    pq.add(v);
                }
            }
        }

        if (dist.get(word2) == Integer.MAX_VALUE) {
            return String.format("No path from \"%s\" to \"%s\"!", word1, word2);
        }
        // 重构路径
        List<String> path = new ArrayList<>();
        for (String at = word2; at != null; at = prev.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return String.format("Shortest path: %s (length = %d)",
                String.join(" -> ", path), dist.get(word2));
    }

    // 5. 计算 PageRank
    public static Double calPageRank(String word) {
        if (!graph.contains(word))
            return null;
        Set<String> nodes = graph.getNodes();
        int N = nodes.size();
        Map<String, Double> pr = new HashMap<>();
        Map<String, Double> tmp = new HashMap<>();
        double init = 1.0 / N;
        // 初始化
        for (String u : nodes)
            pr.put(u, init);

        // 迭代
        for (int iter = 0; iter < 100; iter++) {
            for (String u : nodes) {
                double sum = 0.0;
                // 遍历所有指向 u 的前驱
                for (String v : nodes) {
                    if (graph.getNeighbors(v).containsKey(u)) {
                        int outDeg = graph.getNeighbors(v).size();
                        sum += pr.get(v) / outDeg;
                    }
                }
                tmp.put(u, (1 - DAMPING) / N + DAMPING * sum);
            }
            // 更新
            pr.putAll(tmp);
        }
        return pr.get(word);
    }

    // 6. 随机游走
    public static String randomWalk() {
        Set<String> nodes = graph.getNodes();
        if (nodes.isEmpty())
            return "";
        List<String> nodeList = new ArrayList<>(nodes);
        Random rand = new Random();
        String current = nodeList.get(rand.nextInt(nodeList.size()));
        Set<String> seenEdges = new HashSet<>();
        List<String> walk = new ArrayList<>();
        walk.add(current);

        while (true) {
            Map<String, Integer> nbrs = graph.getNeighbors(current);
            if (nbrs.isEmpty())
                break;
            List<String> choices = new ArrayList<>(nbrs.keySet());
            String next = choices.get(rand.nextInt(choices.size()));
            String edge = current + "->" + next;
            if (seenEdges.contains(edge))
                break;
            seenEdges.add(edge);
            walk.add(next);
            current = next;
        }
        return String.join(" ", walk);
    }

    // ======================== 辅助方法 ========================

    // 读文件、清洗并分词
    private static List<String> readFileAndTokenize(String path) {
        List<String> words = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(" ");
            }
        } catch (IOException e) {
            System.err.println("文件读取错误: " + e.getMessage());
            System.exit(1);
        }
        // 非字母替换为空格，小写
        String cleaned = sb.toString().replaceAll("[^A-Za-z]+", " ").toLowerCase().trim();
        if (!cleaned.isEmpty()) {
            words.addAll(Arrays.asList(cleaned.split("\\s+")));
        }
        return words;
    }

    // 构建有向加权图
    private static void buildGraph(List<String> words) {
        for (int i = 0; i < words.size() - 1; i++) {
            String u = words.get(i), v = words.get(i + 1);
            graph.addEdge(u, v);
        }
    }

    // ======================== 图的数据结构 ========================

    static class DirectedWeightedGraph {
        // 邻接表：u -> (v -> weight)
        private Map<String, Map<String, Integer>> adj = new HashMap<>();

        public void addEdge(String u, String v) {
            adj.computeIfAbsent(u, k -> new HashMap<>())
                    .merge(v, 1, Integer::sum);
            // 确保 v 也在节点集中
            adj.computeIfAbsent(v, k -> new HashMap<>());
        }

        public boolean contains(String u) {
            return adj.containsKey(u);
        }

        public Set<String> getNodes() {
            return adj.keySet();
        }

        public Map<String, Integer> getAdj(String u) {
            return adj.getOrDefault(u, Collections.emptyMap());
        }

        public Map<String, Integer> getNeighbors(String u) {
            return getAdj(u);
        }
    }
}
