### Anomaly Detection using Sub-sequence Clustering

This project provides a demonstration of a simple time-series anomaly detector.

The idea is to use sub-sequence clustering of an EKG signal to reconstruct the EKG.  The difference between
the original and the reconstruction can be used as a measure of how much like the signal is like a prototypical
EKG.  Poor reconstruction can thus be used to find anomalies in the original signal.

The data for this demo are taken from physionet.  See http://physionet.org/physiobank/database/#ecg-databases

The particular data used for this demo is the Apnea ECG database which can be found at

http://physionet.org/physiobank/database/apnea-ecg/

All necessary data for this demo is included as a resource in the source code (see src/main/resources/a02.dat).
You can find original version of the training data at

    http://physionet.org/physiobank/database/apnea-ecg/a02.dat

This file is 6.1MB in size and contains several hours of recorded EKG data from a patient in a sleep apnea study.  This
file contains 3.2 million samples of which we use the first 200,000 for training.

### Installing and Running the Demo
The class com.tdunning.sparse.Learn goes through the steps required to read and process this data to produce a simple
anomaly detector. The output of this program consists of the clustering itself (in dict.tsv) as well as a reconstruction
of the test signal (in trace.tsv).  These outputs can be visualized using the provided R script.

To compile and run the demo,

    mvn -q exec:java -Dexec.mainClass=com.tdunning.sparse.Learn

To produce the figures showing how the anomalies are detected

    rm *.pdf ; Rscript figures.r

### What the Figures Show

Figure 1 shows how an ordinary, non-anomalous signal (top line) is reconstructed (middle line) with relatively small
errors.  Figures 2, 3 and 4 show magnified views of the successive 5 second periods.

Looking at the distribution of the reconstruction error in Figure 5 shows that the error is distinctly not normally
distributed.  Instead, the distribution of the error has longer tails than the normal distribution would have.

Figure 6 shows a histogram of the error.  The standard deviation of the error magnitude is about 5, but nearly 2% of the
errors are larger than 15 (3 standard deviations).  This is implausibly large for a normal distribution which would
only have less than 0.3% of the errors that large.  Even more extreme, 50 samples per million are larger than 20
standard deviations.

Scanning for errors greater than 100 takes us to a point 100 seconds into the recording where the error spikes sharply.
Figure 7 shows the error and Figure 8 shows the original and reconstructed signal for this 5 second period. The
reconstruction clearly isn't capturing the negative excursion of the original signal, but it isn't clear why.  Figure 9
shows a magnified view of the 1 second right around the anomaly and we can see that the problem is a double beat.

Scanning for more anomalies takes us to 240s into the trace where there is a clear signal acquisition malfunction as
shown in Figures 10 and 11.

The 64 most commonly used sub-sequence clusters are shown in figure 12.  The left-most column shows how translations
of the same portion of the heartbeat show up as clusters in the signal dictionary.  These patterns are scaled, shifted
 and added to reconstruct the original signal.

