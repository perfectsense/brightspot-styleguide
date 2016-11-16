package com.psddev.styleguide.viewgenerator;

import com.psddev.dari.util.StringUtils;

interface ViewClassFieldType {

    /**
     * Examples:
     * <ul>
     * <li>com.package.MyClass --> com.package.MyClass</li>
     * <li>com.package.MyClass.MyInnerClass --> com.package.MyClass.MyInnerClass</li>
     * </ul>
     *
     * @return the class name including the package prefix.
     */
    String getFullyQualifiedClassName();

    /**
     * <ul>
     * <li>com.package.MyClass --> MyClass</li>
     * <li>com.package.MyClass.MyInnerClass --> MyInnerClass</li>
     * </ul>
     * @return the simple class name.
     */
    default String getLocalClassName() {

        String className = getClassName();

        int lastDotAt = className.lastIndexOf('.');
        if (lastDotAt >= 0) {
            return className.substring(lastDotAt + 1);
        } else {
            return className;
        }
    }

    /**
     * Examples:
     * <ul>
     * <li>com.package.MyClass --> MyClass</li>
     * <li>com.package.MyClass.MyInnerClass --> MyClass.MyInnerClass</li>
     * </ul>
     * @return the simple class name unless it's an inner class in which case
     *      it is prefixed by its outer class's name separated with a dot.
     */
    default String getClassName() {
        return getFullyQualifiedClassName().substring(getPackageName().length() + 1);
    }

    /**
     * Examples:
     * <ul>
     * <li>com.package.MyClass --> com.package</li>
     * <li>com.package.MyClass.MyInnerClass --> com.package</li>
     * </ul>
     * @return the simple class name unless it's an inner class in which case
     *      it is prefixed by its outer class's name separated with a dot.
     */
    default String getPackageName() {

        StringBuilder builder = new StringBuilder();

        String fullClassName = getFullyQualifiedClassName();

        for (String part : fullClassName.split("\\.")) {

            if (Character.isUpperCase(part.charAt(0))) {
                break;

            } else {
                builder.append(part).append(".");
            }
        }

        return StringUtils.removeSurrounding(builder.toString(), ".");
    }

    /**
     * Examples:
     * <ul>
     * <li>com.package.MyClass1 | com.package.MyClass2 --> true</li>
     * <li>com.package.MyClass1 | com.package.other.MyClass3 --> false</li>
     * </ul>
     * @param relativeFieldType the TemplateFieldType to compare.
     * @return true if this TemplateFieldType has the same {@link #getPackageName()
     *      package} as {@code relativeFieldType}.
     */
    default boolean hasSamePackageAs(ViewClassFieldType relativeFieldType) {
        return getPackageName().equals(relativeFieldType.getPackageName());
    }

    /**
     * Examples:
     * <ul>
     * <li>com.package.MyClass1 | com.package.MyClass2 --> MyClass1</li>
     * <li>com.package.MyClass1 | com.package.other.MyClass3 --> com.package.MyClass1</li>
     * <li>com.package.MyClass1.MyInnerClass1 | com.package.MyClass2 --> MyClass1.MyInnerClass1</li>
     * <li>com.package.MyClass1.MyInnerClass1 | com.package.other.MyClass3 --> com.package.MyClass1.MyInnerClass1</li>
     * </ul>
     *
     * @param relativeFieldType the TemplateFieldType to compare.
     * @return The {@link #getClassName() class name} if the {@code relativeFieldType}
     *      has the same {@link #getPackageName() package} as this one, and
     *      returns the {@link #getFullyQualifiedClassName() fully qualified class}
     *      name otherwise.
     */
    default String getPackageRelativeClassName(ViewClassFieldType relativeFieldType) {
        return hasSamePackageAs(relativeFieldType) ? getClassName() : getFullyQualifiedClassName();
    }

    default boolean contentEquals(ViewClassFieldType other) {
        return getFullyQualifiedClassName().equals(other.getFullyQualifiedClassName());
    }

    static ViewClassFieldType from(String fullyQualifiedClassName) {
        return () -> fullyQualifiedClassName;
    }
}
