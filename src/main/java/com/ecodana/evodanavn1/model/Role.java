package com.ecodana.evodanavn1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Roles")
public class Role {
    @Id
    @Column(name = "RoleId", length = 36)
    private String roleId;
    
    @Column(name = "RoleName", length = 50)
    private String roleName;
    
    @Column(name = "NormalizedName", length = 256)
    private String normalizedName;
    
    // Constructors
    public Role() {}
    
    public Role(String roleId, String roleName, String normalizedName) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.normalizedName = normalizedName;
    }
    
    // Getters/Setters
    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
}
