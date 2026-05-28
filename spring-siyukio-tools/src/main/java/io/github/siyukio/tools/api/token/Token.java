package io.github.siyukio.tools.api.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.With;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Jwt token.
 *
 * @author Budyy
 */

@Builder
@With
public record Token(
        @JsonProperty("jti")
        String jwtId,

        @JsonProperty("sub")
        String subject,

        @JsonProperty("aud")
        String audience,

        @JsonProperty("typ")
        Type type,

        @JsonProperty("prn")
        Principal principal,

        @JsonProperty("act")
        Principal actor,

        @JsonProperty("exp")
        LocalDateTime expirationTime
) {

    public final static String PRINCIPAL_TYPE_USER = "usr";
    public final static String PRINCIPAL_TYPE_APP = "app";
    public final static String PRINCIPAL_TYPE_MEMBER = "mbr";
    public final static String PRINCIPAL_TYPE_INTERNAL = "int";

    public Token(Principal principal) {
        this(null, null, null, Type.ACCESS, principal, null, null);
    }

    public Token(Principal principal, Principal actor) {
        this(null, null, null, Type.ACCESS, principal, actor, null);
    }

    public Token(Principal principal, Type type) {
        this(null, null, null, type, principal, null, null);
    }

    public Token createAccessToken() {
        if (this.type == null || this.type.equals(Type.ACCESS)) {
            throw new IllegalStateException("Only refresh token can be converted to access token.");
        }
        return Token.builder()
                .subject(this.subject).audience(this.audience)
                .type(Type.ACCESS).principal(this.principal)
                .actor(this.actor)
                .build();
    }

    public enum Type {
        @JsonProperty("acc")
        ACCESS,
        @JsonProperty("ref")
        REFRESH
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "typ",
            visible = true
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UserPrincipal.class, name = PRINCIPAL_TYPE_USER),
            @JsonSubTypes.Type(value = MemberPrincipal.class, name = PRINCIPAL_TYPE_MEMBER),
            @JsonSubTypes.Type(value = AppPrincipal.class, name = PRINCIPAL_TYPE_APP),
            @JsonSubTypes.Type(value = InternalPrincipal.class, name = PRINCIPAL_TYPE_INTERNAL)
    })
    public interface Principal {

        String type();

        List<String> scopes();
    }

    public record UserPrincipal(
            @JsonProperty("typ")
            String type,
            @JsonProperty("uid")
            String userId,
            @JsonProperty("unm")
            String userName,
            @JsonProperty("scp")
            List<String> scopes
    ) implements Principal {
        public UserPrincipal(String userId, String userName, String scope) {
            this(PRINCIPAL_TYPE_USER, userId, userName, List.of(scope));
        }

        public UserPrincipal(String userId, String userName, List<String> scopes) {
            this(PRINCIPAL_TYPE_USER, userId, userName, scopes);
        }

        public UserPrincipal(String userId, String userName) {
            this(PRINCIPAL_TYPE_USER, userId, userName, null);
        }
    }

    public record AppPrincipal(
            @JsonProperty("typ")
            String type,
            @JsonProperty("aid")
            String appId,
            @JsonProperty("anm")
            String appName,
            @JsonProperty("scp")
            List<String> scopes
    ) implements Principal {
        public AppPrincipal(String appId, String appName, List<String> scopes) {
            this(PRINCIPAL_TYPE_APP, appId, appName, scopes);
        }

        public AppPrincipal(String appId, String appName) {
            this(PRINCIPAL_TYPE_APP, appId, appName, null);
        }
    }

    public record MemberPrincipal(
            @JsonProperty("typ")
            String type,
            @JsonProperty("gid")
            String groupId,
            @JsonProperty("gnm")
            String groupName,
            @JsonProperty("mid")
            String memberId,
            @JsonProperty("mnm")
            String memberName,
            @JsonProperty("scp")
            List<String> scopes
    ) implements Principal {
        public MemberPrincipal(String groupId, String groupName, String memberId, String memberName, List<String> scopes) {
            this(PRINCIPAL_TYPE_MEMBER, groupId, groupName, memberId, memberName, scopes);
        }

        public MemberPrincipal(String groupId, String groupName, String memberId, String memberName) {
            this(PRINCIPAL_TYPE_MEMBER, groupId, groupName, memberId, memberName, null);
        }

        public MemberPrincipal(String memberId, String memberName, List<String> scopes) {
            this(PRINCIPAL_TYPE_MEMBER, null, null, memberId, memberName, scopes);
        }

        public MemberPrincipal(String memberId, String memberName) {
            this(PRINCIPAL_TYPE_MEMBER, null, null, memberId, memberName, null);
        }
    }

    public record InternalPrincipal(
            @JsonProperty("typ")
            String type,
            @JsonProperty("scp")
            List<String> scopes
    ) implements Principal {
        public InternalPrincipal(List<String> scopes) {
            this(PRINCIPAL_TYPE_INTERNAL, scopes);
        }

        public InternalPrincipal() {
            this(PRINCIPAL_TYPE_INTERNAL, null);
        }
    }

}
