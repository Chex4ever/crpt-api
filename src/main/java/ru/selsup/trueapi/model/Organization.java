package ru.selsup.trueapi.model;

public class Organization {
    private String inn;
    private String status;
    private String productGroups;
    private String is_registered;
    private String is_kfh;

    public Organization(String inn, String status, String productGroups, String is_registered, String is_kfh) {
        this.inn = inn;
        this.status = status;
        this.productGroups = productGroups;
        this.is_registered = is_registered;
        this.is_kfh = is_kfh;
    }

    @Override
    public String toString() {
        return "Organization{" +
                "inn='" + inn + '\'' +
                ", status='" + status + '\'' +
                ", productGroups='" + productGroups + '\'' +
                ", is_registered='" + is_registered + '\'' +
                ", is_kfh='" + is_kfh + '\'' +
                '}';
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInn() {
        return inn;
    }

    public void setInn(String inn) { this.inn = inn; }

    public String getProductGroups() {
        return productGroups;
    }

    public void setProductGroups(String productGroups) {
        this.productGroups = productGroups;
    }

    public String getIs_registered() {
        return is_registered;
    }

    public void setIs_registered(String is_registered) {
        this.is_registered = is_registered;
    }

    public String getIs_kfh() {
        return is_kfh;
    }

    public void setIs_kfh(String is_kfh) {
        this.is_kfh = is_kfh;
    }
}
