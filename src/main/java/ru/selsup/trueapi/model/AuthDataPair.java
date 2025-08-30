package ru.selsup.trueapi.model;

import java.util.Objects;

public final class AuthDataPair {
    public String uuid;
    public String data;

    public AuthDataPair(String uuid, String data) {
        this.uuid = uuid;
        this.data = data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AuthDataPair) obj;
        return Objects.equals(this.uuid, that.uuid) &&
                Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, data);
    }

    @Override
    public String toString() {
        return "Pair[" +
                "UUID=" + uuid + ", " +
                "Data=" + data + ']';
    }

}