package io.github.siyukio.tools.entity.page;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bugee
 */
public class Page<T> {
    
    public int total;

    public List<T> items = new ArrayList<>();
}
