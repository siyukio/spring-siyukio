package io.github.siyukio.tools.entity.query;

/**
 * @author Bugee
 */
public class QueryBuilders {

    public static BoolQueryBuilder boolQuery() {
        return new BoolQueryBuilder();
    }

    public static TermQueryBuilder termQuery(String name, Object value) {
        return new TermQueryBuilder(name, value);
    }

    public static TermsQueryBuilder termsQuery(String name) {
        return new TermsQueryBuilder(name);
    }

    public static TermsQueryBuilder termsQuery(String name, boolean equals, Object... values) {
        return new TermsQueryBuilder(name, equals, values);
    }

    public static RangeQueryBuilder rangeQuery(String name) {
        return new RangeQueryBuilder(name);
    }

    public static MatchQueryBuilder matchQuery(String name, String value) {
        return new MatchQueryBuilder(name, value);
    }

    public static WildcardQueryBuilder wildcardQuery(String name, String value) {
        return new WildcardQueryBuilder(name, value);
    }

    public static WildcardQueryBuilder wildcardPrefixQuery(String name, String value) {
        return new WildcardQueryBuilder(name, value, true, false);
    }

    public static WildcardQueryBuilder wildcardSuffixQuery(String name, String value) {
        return new WildcardQueryBuilder(name, value, false, true);
    }

}
