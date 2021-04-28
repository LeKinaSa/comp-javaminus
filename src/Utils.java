
import pt.up.fe.comp.jmm.JmmNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Utils {
    public static InputStream toInputStream(String text) {
        try {
            return new ByteArrayInputStream(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String generateMethodSignature(JmmNode node) {
        String signature = node.get("name");

        if (signature.equals("main")) {
            signature += "(String[])";
        }
        else {
            Optional<JmmNode> optional = node.getChildren().stream().filter(child -> child.getKind().equals("Params")).findFirst();

            signature += "(";

            if (optional.isPresent()) {
                List<String> types = new ArrayList<>();

                for (JmmNode param : optional.get().getChildren()) {
                    types.add(param.get("type"));
                }

                signature += String.join(", ", types);
            }

            signature += ")";
        }

        return signature;
    }

    public static String generateMethodSignatureFromChildNode(JmmNode node) {
        Optional<JmmNode> methodNode = node.getAncestor("Method");
        return methodNode.map(Utils::generateMethodSignature).orElse(null);
    }
}