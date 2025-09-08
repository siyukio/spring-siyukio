package io.github.siyukio.tools.entity.sort;

/**
 * @author Bugee
 */
public enum SortOrder {

    ASC {
        @Override
        public String toString() {
            return "asc";
        }
    },

    DESC {
        @Override
        public String toString() {
            return "desc";
        }
    };
}

