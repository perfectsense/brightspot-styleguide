package com.psddev.styleguide.viewgenerator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO: Javadocs
 */
final class PathUtils {

    public static final String SLASH = System.getProperty("file.separator");

    @Deprecated
    public static String replaceAllWithSlash(String text, String regex) {
        return text.replaceAll(regex, "\\".equals(SLASH) ? "\\\\" : SLASH);
    }

    public static Path getRelativeCommonPath(Path path, Collection<Path> compareTo) {
        return getRelativeCommonPath(path, compareTo.toArray(new Path[compareTo.size()]));
    }

    public static Path getRelativeCommonPath(Path path, Path... compareTo) {

        if (compareTo != null && compareTo.length > 0) {

            List<String> paths = new ArrayList<>();
            paths.add(path.toString());
            paths.addAll(Arrays.stream(compareTo).map(Path::toString).collect(Collectors.toList()));

            int commonPrefixIndex = getCommonPrefixIndex(paths, '/', '\\');

            return Paths.get(path.toString().substring(commonPrefixIndex));

        } else {
            return path;
        }
    }

    public static int getCommonPrefixIndex(List<String> names, char... commonPrefixIndexCharacters) {

        int namesLength = names.size();

        if (namesLength > 1) {

            Collections.sort(names);

            String first = names.get(0);
            String last = names.get(namesLength - 1);
            int commonLength = first.length();
            int commonIndex = 0;
            int validCommonIndex = 0;

            char lastChar;
            while (commonIndex < commonLength
                    && (lastChar = first.charAt(commonIndex)) == last.charAt(commonIndex)) {

                commonIndex++;

                if (commonPrefixIndexCharacters != null) {
                    for (char c : commonPrefixIndexCharacters) {
                        if (lastChar == c) {
                            validCommonIndex = commonIndex;
                            break;
                        }
                    }
                }
            }

            return commonPrefixIndexCharacters != null ? validCommonIndex : commonIndex;

        } else {
            return 0;
        }
    }
}
