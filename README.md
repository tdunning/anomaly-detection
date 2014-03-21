This project provides implementations of common sparse coding algorithms.

The best illustration of the code so far is an anomaly detection demo.  The idea is to use sub-sequence clustering
of an EKG signal to reconstruct the EKG.  The difference between the original and the reconstruction can be used
to find anomalies in the original signal.

The data for this demo are taken from physionet.  See http://physionet.org/physiobank/database/#ecg-databases

The particular data used for this demo is the Apnea ECG database which can be found at

http://physionet.org/physiobank/database/apnea-ecg/

To run the demo, note that there is a data file included in the resources of this software (see src/main/resources/a02.dat).
You can find original version of this file at

    http://physionet.org/physiobank/database/apnea-ecg/a02.dat

This file is 6.1MB in size and contains several hours of recorded EKG data from a patient in a sleep apnea study.

The class com.tdunning.sparse.Learn goes through the steps required to read and process this data to produce a simple
anomaly detector.
