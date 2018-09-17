package sample.elements;

public class Plate96 {

    private SingleWell[][] wells;

    public Plate96(int barcode){
        wells = new SingleWell[12][8];
        for(int j = 0; j < wells[0].length; j++){
            for(int i = 0; i < wells.length; i++) {
                wells[i][j] = new SingleWell(100, i+1, j+1);
            }
        }
    }

    public void printWells(){
        for(SingleWell[] e : wells){
            System.out.println(e);
        }
    }

}
