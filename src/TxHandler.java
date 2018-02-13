import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
	
	private UTXOPool utxoPool;
	
    public TxHandler(UTXOPool utxoPool) {
    	this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
    	
    	double currentSum = 0;
    	double previouSum = 0;
               
        UTXOPool utxos = new UTXOPool();
        
        for (int i = 0; i < tx.numInputs(); i++) {
        	
            Transaction.Input inputOfTx = tx.getInput(i);
            UTXO utxo = new UTXO(inputOfTx.prevTxHash, inputOfTx.outputIndex);
            Transaction.Output outputTx = utxoPool.getTxOutput(utxo);
            
            //all outputs claimed by {@code tx} are in the current UTXO pool
            if (!utxoPool.contains(utxo)) return false;
            
            //the signatures on each input of {@code tx} are valid
            if (!Crypto.verifySignature(outputTx.address, tx.getRawDataToSign(i), inputOfTx.signature))
                return false;
            
            //no UTXO is claimed multiple times by {@code tx}
            if (utxos.contains(utxo)) return false;
            
            utxos.addUTXO(utxo, outputTx);
            previouSum += outputTx.value;
        }
        for (Transaction.Output out : tx.getOutputs()) {
        	
        	//all of {@code tx}s output values are non-negative
            if (out.value < 0) return false;
            currentSum += out.value;
        }
        
        //the sum of {@code tx}s input values is greater than or equal to the sum of its output values
        return previouSum >= currentSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
    	
    	Set<Transaction> validTxs = new HashSet<>();

        for (Transaction tx : possibleTxs) {
        	
        	//Check whether each transaction is valid
            if (isValidTx(tx)) {
            	
                validTxs.add(tx);
                
                for (Transaction.Input inputOfTx : tx.getInputs()) {
                    UTXO utxo = new UTXO(inputOfTx.prevTxHash, inputOfTx.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output out = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(utxo, out);
                }
            }
        }

        //mutually valid array of accepted transactions
        Transaction[] acceptedTxs = new Transaction[validTxs.size()];
        return validTxs.toArray(acceptedTxs);
    }

}
