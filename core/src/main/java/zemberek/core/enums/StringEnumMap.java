package zemberek.core.enums;

import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * This is a convenience class for enums that also are represented with strings.
 * This classes can be useful for loading enum values from textual data.
 *
 * @param <T>
 */
public class StringEnumMap<T extends StringEnum> {
    private final ImmutableMap<String, T> map;
    private final Class<T> clazz;
    private boolean ignoreCase = false;

    private StringEnumMap(Class<T> clazz) {
        this(clazz, true);
    }

    private StringEnumMap(Class<T> clazz, boolean ignoreCase) {
        this.clazz = clazz;
        final ImmutableMap.Builder<String, T> mapBuilder = new ImmutableMap.Builder<>();
        for (T senum : clazz.getEnumConstants()) {
            mapBuilder.put(senum.getStringForm(), senum);
            if (ignoreCase) {
                String lowerCase = senum.getStringForm().toLowerCase(Locale.ENGLISH);
                if (!lowerCase.equals(senum.getStringForm())) {
                    mapBuilder.put(lowerCase, senum);
                }
            }
        }
        this.map = mapBuilder.build();
        this.ignoreCase = ignoreCase;
    }

    public static <T extends StringEnum> StringEnumMap<T> get(Class<T> clazz) {
        return new StringEnumMap<>(clazz);
    }

    public T getEnum(String s) {
        if (Strings.isNullOrEmpty(s))
            throw new IllegalArgumentException("Input String must have content.");
        T res = map.get(s);
        if (res == null)
            throw new IllegalArgumentException("Cannot find a representation of :" + s + " for enum class:" + clazz.getName());
        return res;
    }

    public Set<String> getKeysSet() {
        return map.keySet();
    }

    public Collection<T> getEnums(Collection<String> strings) {
        if (strings == null || strings.isEmpty())
            return Collections.emptyList();
        else
            return Collections2.transform(strings, this::getEnum);
    }

    public boolean enumExists(String s) {
        return ignoreCase ? map.containsKey(s) || map.containsKey(s.toLowerCase(Locale.ENGLISH)) : map.containsKey(s);
    }
}
