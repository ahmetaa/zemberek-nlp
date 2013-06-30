package zemberek.core.enums;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Collections;

/**
 * This is a convenience class for enums that also are represented with strings.
 * This classes can be useful for loading enum values from textual data.
 *
 * @param <T>
 */
public class StringEnumMap<T extends StringEnum> {
    private final ImmutableMap<String, T> map;
    private final Class<T> clazz;

    private StringEnumMap(Class<T> clazz) {
        this.clazz = clazz;
        final ImmutableMap.Builder<String, T> mapBuilder = new ImmutableMap.Builder<>();
        for (T senum : clazz.getEnumConstants()) {
            mapBuilder.put(senum.getStringForm(), senum);
        }
        this.map = mapBuilder.build();
    }

    public static <T extends StringEnum> StringEnumMap<T> get(Class<T> clazz) {
        return new StringEnumMap<T>(clazz);
    }

    public T getEnum(String s) {
        if (Strings.isNullOrEmpty(s))
            throw new IllegalArgumentException("Input String must have content.");
        T res = map.get(s);
        if (res == null)
            throw new IllegalArgumentException("Cannot find a representation of :" + s + " for enum class:" + clazz.getName());
        return res;
    }

    public Collection<T> getEnums(Collection<String> strs) {
        if (strs == null || strs.isEmpty())
            return Collections.emptyList();
        else
            return Collections2.transform(strs, new Function<String, T>() {
                @Override
                public T apply(String input) {
                    return getEnum(input);
                }
            });
    }

    public boolean enumExists(String s) {
        return map.containsKey(s);
    }
}
