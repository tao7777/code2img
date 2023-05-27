package org.example;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.printer.XmlPrinter;
import com.github.javaparser.printer.YamlPrinter;
import com.google.common.hash.Hashing;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import static org.example.Main.*;
import static org.example.TypeMappings.*;

public class Func {

    public int funcId;
    public String fileName;
    public int funcLen;
    public float[][] image;
    public int[][] intImage;
    public short[][] matrix;

    public short[] vector;

    public int edgeNum;
    public List<Long> nLineHash;


    @Deprecated
    public Func(String fileName, int funcLen, CompilationUnit cu, List<String> normLines) {
        this.fileName = fileName;
        this.funcLen = funcLen;
        setNLineHash(normLines);
        setFuncMatrix(cu);
    }

    public Func(String fileName, CompilationUnit cu) {
        this.fileName = fileName;
        setNLineHashAndVector(cu);
    }

    public static List<String> getNormLines(CompilationUnit cu) {
        List<String> normLines = new ArrayList<>();
        cu.walk(Node.TreeTraversal.BREADTHFIRST, node -> {
            if (node.getChildNodes().size() == 0) {
                if (SimpleName.class.equals(node.getClass())) {
                    SimpleName sn = (SimpleName) node;
                    sn.setId("SimpleName");
                } else if (Name.class.equals(node.getClass())) {
                    Name n = (Name) node;
                    n.setId("Name");
                } else if (StringLiteralExpr.class.equals(node.getClass())) {
                    StringLiteralExpr sle = (StringLiteralExpr) node;
                    sle.setString("StringLiteralExpr");
                } else if (BooleanLiteralExpr.class.equals(node.getClass())) {
                    BooleanLiteralExpr ble = (BooleanLiteralExpr) node;
                    ble.setValue(false);
                } else if (IntegerLiteralExpr.class.equals(node.getClass())) {
                    IntegerLiteralExpr ile = (IntegerLiteralExpr) node;
                    ile.setValue("0");
                } else if (DoubleLiteralExpr.class.equals(node.getClass())) {
                    DoubleLiteralExpr dle = (DoubleLiteralExpr) node;
                    dle.setValue("0.0");
                }
            }
        });
        var lines = cu.toString().split("\\r?\\n");
        if (lines.length <= 3) {
            return normLines;
        }
        for (int i = 2; i < lines.length - 1; i++) {
            var line = lines[i].strip();
            if (!line.isEmpty()) {
                normLines.add(line);
            }
        }
        return normLines;
    }

    @Deprecated
    public static float nLineVerify(Func funcA, Func funcB, Map<Long, HashSet<Integer>> invertedIndex) {
        var res = commonNLine(funcA, funcB, invertedIndex);
        int min_len = Math.min(funcA.funcLen, funcB.funcLen) - N + 1;
        return 1.0f * res / min_len;
    }

    public static float nLineVerify(Func funcA, Func funcB, InvertedIndex invertedIndex) {
        var res = commonNLine(funcA, funcB, invertedIndex);
        int min_len = Math.min(funcA.funcLen, funcB.funcLen) - N + 1;
        return 1.0f * res / min_len;
    }

    @Deprecated
    public static int commonNLine(Func funcA, Func funcB, Map<Long, HashSet<Integer>> invertedIndex) {
        int res = 0;
        if (funcB.funcLen > funcA.funcLen) {
            Func tmp = funcB;
            funcB = funcA;
            funcA = tmp;
        }
        for (var lineHash : funcB.nLineHash) {
            if (invertedIndex.containsKey(lineHash) && invertedIndex.get(lineHash).contains(funcA.funcId)) {
                res += 1;
            }
        }
        return res;
    }

    public static int commonNLine(Func funcA, Func funcB, InvertedIndex invertedIndex) {
        int res = 0;
        if (funcB.funcLen > funcA.funcLen) {
            Func tmp = funcB;
            funcB = funcA;
            funcA = tmp;
        }
        for (var hash : funcB.nLineHash) {
            if (invertedIndex.check(hash, funcA.funcId)) {
                res += 1;
            }
        }
        return res;
    }

    public static float matrixVerify(Func funcA, Func funcB) {
        return calMSE(funcA.matrix, funcB.matrix);
    }

    public static double matrixCosVerify(Func funcA, Func funcB) {
        var matrix1 = funcA.matrix;
        var matrix2 = funcB.matrix;
        long a = 0;
        long b = 0;
        long c = 0;
        for (int i = 0; i < typeNum1; i++) {
            for (int j = 0; j < typeNum2; j++) {
                var x = matrix1[i][j];
                var y = matrix2[i][j];
                a += x * y;
                b += x * x;
                c += y * y;
            }
        }
        return 1.0 * a / Math.sqrt(b * c);
    }

    public static double matrixJacVerify(Func funcA, Func funcB) {
        var matrix1 = funcA.matrix;
        var matrix2 = funcB.matrix;
        int ret = 0;
        for (int i = 0; i < typeNum1; i++) {
            for(int j = 0; j < typeNum2; j++) {
                if(matrix1[i][j] == matrix2[i][j]) {
                    ret += 1;
                }
            }
        }
        return 1.0 * ret / (typeNum1 * typeNum2 * 2 - ret);
    }


    public static double matrixGenJacVerify(Func funcA, Func funcB) {
        var matrix1 = funcA.matrix;
        var matrix2 = funcB.matrix;
        int a = 0, b = 0, c = 0;
        for (int i = 0; i < typeNum1; i++) {
            for(int j = 0; j < typeNum2; j++) {
                a += matrix1[i][j] * matrix2[i][j];
                b += matrix1[i][j] * matrix1[i][j];
                c += matrix2[i][j] * matrix2[i][j];
            }
        }
        return 1.0 * a / (b + c - a);
    }

    public static double imageCosVerify(Func funcA, Func funcB) {
        var matrix1 = funcA.image;
        var matrix2 = funcB.image;
        double a = 0;
        double b = 0;
        double c = 0;
        for (int i = 0; i < typeNum1; i++) {
            for (int j = 0; j < typeNum2; j++) {
                var x = matrix1[i][j];
                var y = matrix2[i][j];
                a += x * y;
                b += x * x;
                c += y * y;
            }
        }
        return a / Math.sqrt(b * c);
    }

    public static double imageJacVerify(Func funcA, Func funcB) {
        var image1 = funcA.image;
        var image2 = funcB.image;
        int ret = 0;
        for (int i = 0; i < typeNum1; i++) {
            for(int j = 0; j < typeNum2; j++) {
                if(image1[i][j] == image2[i][j]) {
                    ret += 1;
                }
            }
        }
        return 1.0 * ret / (typeNum1 * typeNum2 * 2 - ret);
    }
    public static double imageGenJacVerify(Func funcA, Func funcB) {
        var image1 = funcA.image;
        var image2 = funcB.image;
        double a = 0, b = 0, c = 0;
        for (int i = 0; i < typeNum1; i++) {
            for(int j = 0; j < typeNum2; j++) {
                a += image1[i][j] * image2[i][j];
                b += image1[i][j] * image1[i][j];
                c += image2[i][j] * image2[i][j];
            }
        }
        return a / (b + c - a);
    }


    public static double imageVerify(Func funcA, Func funcB) {
//        var imageA = generateImageFromMatrix(funcA.matrix);
//        var imageB = generateImageFromMatrix(funcB.matrix);
        return Math.sqrt(calMSE(funcA.image, funcB.image));
    }

    public static double intImageVerify(Func funcA, Func funcB) {
        return Math.sqrt(calMSE(funcA.intImage, funcB.intImage));
    }

    public static double vectorCosVerify(Func funcA, Func funcB) {
        var vec1 = funcA.vector;
        var vec2 = funcB.vector;
        long a = 0, b = 0, c = 0;
        for (int i = 0; i < edgeTypeNum; i++) {
            a += vec1[i] * vec2[i];
            b += vec1[i] * vec1[i];
            c += vec2[i] * vec2[i];
        }
        return 1.0 * a / Math.sqrt(b * c);
    }

    public static double vectorJacVerify(Func funcA, Func funcB) {
        var vec1 = funcA.vector;
        var vec2 = funcB.vector;
        int common = 0;
        for (int i = 0; i < edgeTypeNum; i++) {
            if (vec1[i] == vec2[i]) {
                common++;
            }
        }
        return 1.0 * common / (2 * edgeTypeNum - common);
    }

    public static double vectorGenJacVerify(Func funcA, Func funcB) {
        var vec1 = funcA.vector;
        var vec2 = funcB.vector;
        long a = 0, b = 0, c = 0;
        for (int i = 0; i < edgeTypeNum; i++) {
            a += vec1[i] * vec2[i];
            b += vec1[i] * vec1[i];
            c += vec2[i] * vec2[i];
        }
        return 1.0 * a / (b + c - a);
    }


    public static double normVecCosVerify(Func funcA, Func funcB) {
        var vec1 = funcA.vector;
        var sum1 = funcA.edgeNum;
        var vec2 = funcB.vector;
        var sum2 = funcB.edgeNum;
        int _a = 0, _b = 0, _c = 0;
        for (int i = 0; i < edgeTypeNum; i++) {
            _a += vec1[i] * vec2[i];
            _b += vec1[i] * vec1[i];
            _c += vec2[i] * vec2[i];
        }
        double a = 1.0 * _a / (sum1 * sum2);
        double b = 1.0 * _b / (sum1 * sum1);
        double c = 1.0 * _c / (sum2 * sum2);
        return a / Math.sqrt(b * c);
    }

    public static double normVecJacVerify(Func funcA, Func funcB) {
        var vec1 = funcA.vector;
        var sum1 = funcA.edgeNum;
        var vec2 = funcB.vector;
        var sum2 = funcB.edgeNum;
        int common = 0;
        for (int i = 0; i < edgeTypeNum; i++) {
            double a = 1.0 * vec1[i] / sum1;
            double b = 1.0 * vec2[i] / sum2;
            if (a == b) {
                common++;
            }
        }
        return 1.0 * common / (2 * edgeTypeNum - common);
    }

    public static double normVecGenJacVerify(Func funcA, Func funcB) {
        var vec1 = funcA.vector;
        var sum1 = funcA.edgeNum;
        var vec2 = funcB.vector;
        var sum2 = funcB.edgeNum;
        int _a = 0, _b = 0, _c = 0;
        for (int i = 0; i < edgeTypeNum; i++) {
            _a += vec1[i] * vec2[i];
            _b += vec1[i] * vec1[i];
            _c += vec2[i] * vec2[i];
        }
        double a = 1.0 * _a / (sum1 * sum2);
        double b = 1.0 * _b / (sum1 * sum1);
        double c = 1.0 * _c / (sum2 * sum2);
        return a / (b + c - a);
    }

    public static double vectorDisVerify(Func funcA, Func funcB) {
        var vec1 = funcA.vector;
        var vec2 = funcB.vector;
        long res = 0;
        for (int i = 0; i < edgeTypeNum; i++) {
            res += (long) (vec1[i] - vec2[i]) * (vec1[i] - vec2[i]);

        }
        return Math.sqrt(res);
    }

    private static float[][] generateImageFromMatrix(short[][] matrix) {
        float[][] image = new float[typeNum1][typeNum2];
        for (int i = 0; i < typeNum1; i++) {
            int sum = 0;
            for (int j = 0; j < typeNum2; j++) {
                sum += matrix[i][j];
            }
            if (sum > 0) {
                for (int j = 0; j < typeNum2; j++) {
                    image[i][j] = 1.0f * matrix[i][j] / sum;
                }
            }
        }
        return image;
    }

    private static int[][] generateIntImageFromMatrix(short[][] matrix) {
        int[][] image = new int[typeNum1][typeNum2];
        for (int i = 0; i < typeNum1; i++) {
            int sum = 0;
            for (int j = 0; j < typeNum2; j++) {
                sum += matrix[i][j];
            }
            if (sum > 0) {
                for (int j = 0; j < typeNum2; j++) {
                    image[i][j] = 1000 * matrix[i][j] * 255 / sum;
                }
            }
        }
        return image;
    }

    private static float calMSE(float[][] a, float[][] b) {
        int ret = 0;
        for (int i = 0; i < typeNum1; i++) {
            for (int j = 0; j < typeNum2; j++) {
                ret += (a[i][j] - b[i][j]) * (a[i][j] - b[i][j]);
            }
        }
        return 1.0f * ret / (typeNum1 * typeNum2);
//        return ret;
    }

    private static float calMSE(short[][] a, short[][] b) {
        int ret = 0;
        for (int i = 0; i < typeNum1; i++) {
            for (int j = 0; j < typeNum2; j++) {
                ret += (a[i][j] - b[i][j]) * (a[i][j] - b[i][j]);
            }
        }
        return 1.0f * ret / (typeNum1 * typeNum2);
//        return ret;
    }

    private static float calMSE(int[][] a, int[][] b) {
        int ret = 0;
        for (int i = 0; i < typeNum1; i++) {
            for (int j = 0; j < typeNum2; j++) {
                ret += (a[i][j] - b[i][j]) * (a[i][j] - b[i][j]);
            }
        }
        return 1.0f * ret / (typeNum1 * typeNum2);
//        return ret;
    }

    private void setNLineHashAndMatrix(CompilationUnit cu) {
        short[][] matrix = new short[typeNum1][typeNum2];
        List<String> normLines = new ArrayList<>();
        Queue<Node> queue = new LinkedList<>();
        queue.add(cu);
        while (!queue.isEmpty()) {
//            var head = stack.pop();
            var head = queue.poll();
            var t1 = type2Int1.get(head.getClass().getSimpleName()) == null ? 0 : (int) type2Int1.get(head.getClass().getSimpleName());
            var children = head.getChildNodes();
            if (children.isEmpty()) {
                if (SimpleName.class.equals(head.getClass())) {
                    SimpleName sn = (SimpleName) head;
                    sn.setId("VAR1");
                } else if (Name.class.equals(head.getClass())) {
                    Name n = (Name) head;
                    n.setId("VAR2");
                } else if (StringLiteralExpr.class.equals(head.getClass())) {
                    StringLiteralExpr sle = (StringLiteralExpr) head;
                    sle.setString("STR");
                } else if (BooleanLiteralExpr.class.equals(head.getClass())) {
                    BooleanLiteralExpr ble = (BooleanLiteralExpr) head;
                    ble.setValue(false);
                } else if (IntegerLiteralExpr.class.equals(head.getClass())) {
                    IntegerLiteralExpr ile = (IntegerLiteralExpr) head;
                    ile.setValue("0");
                } else if (DoubleLiteralExpr.class.equals(head.getClass())) {
                    DoubleLiteralExpr dle = (DoubleLiteralExpr) head;
                    dle.setValue("0.0");
                }
                continue;
            }
            for (var child : children) {
//                stack.push(child);
                queue.offer(child);
                var t2 = type2Int2.get(child.getClass().getSimpleName()) == null ? 0 : (int) type2Int2.get(child.getClass().getSimpleName());
                matrix[t1][t2] += 1;
                if (matrix[t1][t2] < 0) {
                    System.out.println(this.fileName + ": " + t1 + "->" + t2 + "reached cap!");
                    matrix[t1][t2] = Short.MAX_VALUE;
                }
            }
        }

//        YamlPrinter yamlPrinter = new YamlPrinter(true);
//        try (FileWriter fileWriter = new FileWriter("out2.yaml");
//             PrintWriter printWriter = new PrintWriter(fileWriter)) {
//            printWriter.write(yamlPrinter.output(cu));
//        } catch (Exception e) {
//        }
//        System.out.println(yamlPrinter.output(cu));
//
//
//        XmlPrinter xmlPrinter = new XmlPrinter(true);
//        try (FileWriter fileWriter = new FileWriter("out2.xml");
//             PrintWriter printWriter = new PrintWriter(fileWriter)) {
//            printWriter.write(xmlPrinter.output(cu));
//        } catch (Exception e) {
//        }

        var lines = cu.toString().split("\\r?\\n");
        for (int i = 2; i < lines.length - 1; i++) {
            var line = lines[i].strip();
            if (!line.isEmpty()) {
                normLines.add(line);


//                System.out.println(line);


            }
        }
        this.funcLen = normLines.size();
        setNLineHash(normLines);
        this.matrix = matrix;
//        this.intImage = generateIntImageFromMatrix(matrix);
        this.image = generateImageFromMatrix(matrix);
    }


    private void setNLineHashAndVector(CompilationUnit cu) {
        short[] vector = new short[edgeTypeNum];
        List<String> normLines = new ArrayList<>();
        Queue<Node> queue = new LinkedList<>();
        queue.add(cu);
        int totalEdges = 0;
//        Map<String, Integer> ret = new HashMap<>();
        while (!queue.isEmpty()) {
//            var head = stack.pop();
            var head = queue.poll();
//            var t1 = type2Int1.get(head.getClass().getSimpleName()) == null ? 0 : (int) type2Int1.get(head.getClass().getSimpleName());
            var str1 = head.getClass().getSimpleName();
            var children = head.getChildNodes();
            if (children.isEmpty()) {
                if (SimpleName.class.equals(head.getClass())) {
                    SimpleName sn = (SimpleName) head;
                    sn.setId("VAR1");
                } else if (Name.class.equals(head.getClass())) {
                    Name n = (Name) head;
                    n.setId("VAR2");
                } else if (StringLiteralExpr.class.equals(head.getClass())) {
                    StringLiteralExpr sle = (StringLiteralExpr) head;
                    sle.setString("STR");
                } else if (BooleanLiteralExpr.class.equals(head.getClass())) {
                    BooleanLiteralExpr ble = (BooleanLiteralExpr) head;
                    ble.setValue(false);
                } else if (IntegerLiteralExpr.class.equals(head.getClass())) {
                    IntegerLiteralExpr ile = (IntegerLiteralExpr) head;
                    ile.setValue("0");
                } else if (DoubleLiteralExpr.class.equals(head.getClass())) {
                    DoubleLiteralExpr dle = (DoubleLiteralExpr) head;
                    dle.setValue("0.0");
                }
                continue;
            }
            for (var child : children) {
//                stack.push(child);
                queue.offer(child);
                var str2 = child.getClass().getSimpleName();
                var index = edgeType2Num.get(str1 + str2) == null ? 0 : (int) edgeType2Num.get(str1 + str2);
                vector[index] += 1;
                totalEdges += 1;

//                String stringIndex = str1 + "," + str2;
//                if(ret.get(stringIndex) == null) {
//                    ret.put(stringIndex, 1);
//                } else {
//                    var v = ret.get(stringIndex);
//                    ret.put(stringIndex, v + 1);
//                }

            }
        }

//        for (var s : ret.entrySet()) {
//            System.out.println(s.getKey() + "," + s.getValue());
//        }


        var lines = cu.toString().split("\\r?\\n");
        for (int i = 2; i < lines.length - 1; i++) {
            var line = lines[i].strip();
            if (!line.isEmpty()) {
                normLines.add(line);
//                System.out.println(line);
            }
        }
        this.funcLen = normLines.size();
        setNLineHash(normLines);
        this.vector = vector;
        this.edgeNum = totalEdges;
    }


    public void setFuncId(int funcId) {
        this.funcId = funcId;
    }

    private void setNLineHash(List<String> normLines) {
        List<Long> nLineHash = new ArrayList<>();
        int len = normLines.size() - N + 1;
        for (int i = 0; i < len; i++) {
            StringBuilder tmp = new StringBuilder();
            for (int j = 0; j < N; j++) {
//                tmp = tmp.concat(normLines.get(i + j));
                tmp.append(normLines.get(i + j));
            }
//            nLineHash.add(Hashing.murmur3_128().hashBytes(tmp.toString().getBytes()));
//            nLineHash.add(tmp.toString().hashCode());
            nLineHash.add(Hashing.sipHash24().hashBytes(tmp.toString().getBytes()).asLong());
//            nLineHash.add(Hashing.murmur3_32_fixed().hashBytes(tmp.toString().getBytes()).asInt());
//            nLineHash.add(XXHashFactory.fastestInstance().hash64().hash(ByteBuffer.wrap(tmp.toString().getBytes()), seed));
        }
        this.nLineHash = nLineHash;
//        return nLineHash;
    }

    @Deprecated
    private void setFuncMatrix(CompilationUnit cu) {
        short[][] matrix = new short[typeNum1][typeNum2];
        Queue<Node> queue = new LinkedList<>();
        queue.add(cu);
        while (!queue.isEmpty()) {
//            var head = stack.pop();
            var head = queue.poll();
            var t1 = type2Int1.get(head.getClass().getSimpleName()) == null ? 0 : (int) type2Int1.get(head.getClass().getSimpleName());
            var children = head.getChildNodes();
            for (var child : children) {
//                stack.push(child);
                queue.offer(child);
                var t2 = type2Int2.get(child.getClass().getSimpleName()) == null ? 0 : (int) type2Int2.get(child.getClass().getSimpleName());
                matrix[t1][t2] += 1;
                if (matrix[t1][t2] < 0) {
                    System.out.println(this.fileName + ": " + t1 + "->" + t2 + "reached cap!");
                    matrix[t1][t2] = Short.MAX_VALUE;
                }
            }
        }
//        this.matrix = matrix;
        this.intImage = generateIntImageFromMatrix(matrix);
    }
}
