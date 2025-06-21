package io.github.siyukio.tools.api.parameter.response;

/**
 * @author Buddy
 */
public final class BasicResponseFilter implements ResponseFilter {

    private final String name;

    public BasicResponseFilter(String name) {
        this.name = name;
    }

    @Override
    public void filter(Object value) {
    }

    @Override
    public String getName() {
        return this.name;
    }
}
