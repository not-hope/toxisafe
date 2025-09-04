package com.toxisafe.model;

public class Usuario {
    private String idUsuario;
    private String nombreUsuario;
    private String rolUsuario;
    private String usernameUsuario;
    private String passwordUsuario;

    // Constructor
    public Usuario(String idUsuario, String nombreUsuario, String rolUsuario,
                   String usernameUsuario, String passwordUsuario) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.rolUsuario = rolUsuario;
        this.usernameUsuario = usernameUsuario;
        this.passwordUsuario = passwordUsuario;
    }

    // Getters y Setters
    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getRolUsuario() { return rolUsuario; }
    public void setRolUsuario(String rolUsuario) { this.rolUsuario = rolUsuario; }
    public String getUsernameUsuario() { return usernameUsuario; }
    public void setUsernameUsuario(String usernameUsuario) { this.usernameUsuario = usernameUsuario; }
    public String getPasswordUsuario() { return passwordUsuario; }
    public void setPasswordUsuario(String passwordUsuario) { this.passwordUsuario = passwordUsuario; }

    @Override
    public String toString() {
        return "Usuario{" +
                "idUsuario='" + idUsuario + '\'' +
                ", nombreUsuario='" + nombreUsuario + '\'' +
                ", rolUsuario='" + rolUsuario + '\'' +
                ", usernameUsuario='" + usernameUsuario + '\'' +
                '}';
    }
}

