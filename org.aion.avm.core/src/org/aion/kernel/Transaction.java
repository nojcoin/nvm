package org.aion.kernel;

import org.aion.avm.core.util.Helpers;

import java.util.Arrays;
import java.util.Objects;

public class Transaction {

    public enum Type {
        CREATE, CALL
    }

    Type type;

    byte[] from;

    byte[] to;

    long nonce;

    long value;

    byte[] data;

    long energyLimit;

    long energyPrice;

    public Transaction(Type type, byte[] from, byte[] to, long nonce, long value, byte[] data, long energyLimit, long energyPrice) {
        Objects.requireNonNull(type, "The transaction `type` can't be NULL");
        Objects.requireNonNull(from, "The transaction `from` can't be NULL");
        if (type == Type.CREATE) {
           if (to != null) {
               throw new IllegalArgumentException("The transaction `to` has to be NULL for CREATE");
           }
        } else {
            Objects.requireNonNull(to, "The transaction `to`  can't be NULL for non-CREATE");
        }

        this.type = type;
        this.from = from;
        this.to = to;
        this.nonce = nonce;
        this.value = value;
        this.data = data;
        this.energyLimit = energyLimit;
        this.energyPrice = energyPrice;
    }

    public Type getType() {
        return type;
    }

    public byte[] getFrom() {
        return from;
    }

    public byte[] getTo() {
        return to;
    }

    public long getNonce() {
        return nonce;
    }

    public long getValue() {
        return value;
    }

    public byte[] getData() {
        return data;
    }

    public long getEnergyLimit() {
        return energyLimit;
    }

    public long getEnergyPrice() {
        return energyPrice;
    }

    public int getBasicCost() {
        int cost = 21_000;
        for (byte b : getData()) {
            cost += (b == 0) ? 4 : 64;
        }
        return cost;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "type=" + type +
                ", from=" + Helpers.bytesToHexString(Arrays.copyOf(from, 4)) +
                ", to=" + Helpers.bytesToHexString(Arrays.copyOf(to, 4)) +
                ", value=" + value +
                ", data=" + Helpers.bytesToHexString(data) +
                ", energyLimit=" + energyLimit +
                '}';
    }
}
