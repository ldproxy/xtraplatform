package de.ii.xtraplatform.auth.external;

import com.google.common.base.Strings;

import java.util.List;
import java.util.Objects;

/**
 * @author zahnen
 */
public class XacmlResponse {
    public List<Decision> Response;

    boolean isAllowed() {
        return Objects.nonNull(Response) && !Response.isEmpty() && Strings.nullToEmpty(Response.get(0).Decision).equals("Permit");
    }

    static class Decision {
        public String Decision;
    }
}
