package com.jarvis.assistant.banking;

/** Phase 64: deterministic pre-transfer review snapshot. */
public final class TransferReview {
    private final String bankName, sourceCardMasked, destinationCardMasked,
            destinationIbanMasked, note;
    private final long amount;

    public TransferReview(String bankName, String sourceCardMasked,
                          String destinationCardMasked, String destinationIbanMasked,
                          long amount, String note) {
        this.bankName = safe(bankName);
        this.sourceCardMasked = safe(sourceCardMasked);
        this.destinationCardMasked = safe(destinationCardMasked);
        this.destinationIbanMasked = safe(destinationIbanMasked);
        this.amount = amount;
        this.note = safe(note);
    }
    public String getBankName(){return bankName;}
    public String getSourceCardMasked(){return sourceCardMasked;}
    public String getDestinationCardMasked(){return destinationCardMasked;}
    public String getDestinationIbanMasked(){return destinationIbanMasked;}
    public long getAmount(){return amount;}
    public String getNote(){return note;}
    private static String safe(String v){return v==null?"":v;}
}
