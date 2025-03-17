package org.sitenetsoft.sunseterp.framework.entity.datasource;

/**
 * A container for data source connection information.
 * <p><b>Note that this class is not synchronized.</b>
 * If multiple threads access a <code>GenericHelperInfo</code> concurrently it must be synchronized externally.
 *
 */
public final class GenericHelperInfo {
    private final String entityGroupName;
    private final String helperBaseName;
    private String tenantId = "";
    private String overrideJdbcUri = "";
    private String overrideUsername = "";
    private String overridePassword = "";
    private String helperFullName = "";

    public GenericHelperInfo(String entityGroupName, String helperBaseName) {
        this.entityGroupName = entityGroupName == null ? "" : entityGroupName;
        this.helperBaseName = helperBaseName == null ? "" : helperBaseName;
        this.helperFullName = this.helperBaseName;
    }

    public String getHelperFullName() {
        return helperFullName;
    }

    public String getEntityGroupName() {
        return entityGroupName;
    }

    public String getHelperBaseName() {
        return helperBaseName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        if (tenantId != null) {
            this.tenantId = tenantId;
            helperFullName = helperBaseName.concat("#").concat(tenantId);
        }
    }

    public String getOverrideJdbcUri() {
        return overrideJdbcUri;
    }

    public String getOverrideJdbcUri(String defaultValue) {
        return overrideJdbcUri.isEmpty() ? defaultValue : overrideJdbcUri;
    }

    public void setOverrideJdbcUri(String overrideJdbcUri) {
        if (overrideJdbcUri != null) {
            this.overrideJdbcUri = overrideJdbcUri;
        }
    }

    public String getOverrideUsername() {
        return overrideUsername;
    }

    public String getOverrideUsername(String defaultValue) {
        return overrideUsername.isEmpty() ? defaultValue : overrideUsername;
    }

    public void setOverrideUsername(String overrideUsername) {
        if (overrideUsername != null) {
            this.overrideUsername = overrideUsername;
        }
    }

    public String getOverridePassword() {
        return overridePassword;
    }

    public String getOverridePassword(String defaultValue) {
        return overridePassword.isEmpty() ? defaultValue : overridePassword;
    }

    public void setOverridePassword(String overridePassword) {
        if (overridePassword != null) {
            this.overridePassword = overridePassword;
        }
    }
}
