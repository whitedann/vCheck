package sample.elements;

public class SingleWell {

    private double desiredVolume, measuredVolume;
    private int row, col;
    private WellState mState;
    private static final double lowerBound = 0.8;
    private static final double upperBound = 1.2;

    public SingleWell(double desiredVolume, int row, int col){
        this.row = row;
        this.col = col;
        this.desiredVolume = desiredVolume;
        this.mState = WellState.NODATA;
    }

    public void analyzeWell(){
        if(this.measuredVolume <= this.desiredVolume*lowerBound)
            this.mState = WellState.FAIL;
        else if(this.measuredVolume >= this.desiredVolume*upperBound)
            this.mState = WellState.FAIL;
        else
            this.mState = WellState.PASS;
    }
}
