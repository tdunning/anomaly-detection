figure = 1
ylim = c(-180, 180)

if (!exists('xtrace')) {
  xtrace = read.delim("trace.tsv", header=F)
  dict = read.delim("dict.tsv", header=F)
}

# plots three graphs in a single figure.  These graphs contain
# 1) the original signal
# 2) the reconstructed signal
# 3) the error
# The offset argument indicates how many samples the plots should be offset into the entire trace
threePlot = function (offset) {
  startPdf()
  xm = matrix(c(0,1,2,3,0),ncol=3, nrow=5)
  xm[,1] = 0
  xm[,3] = 0
  layout(xm, heights=c(0.2, 1, 1, 1.5, 0.02), widths = c(0.1, 1, 0.1))
  par(mar=c(1,1,0,0.2))
  plot(xtrace$V1[offset + 1:1000], type='l', ylim=ylim, ylab='', xaxt='n', yaxt='n')
  axis(side=2, at=c(-100, 0, 100))
  plot(xtrace$V2[offset + 1:1000], type='l', ylim=ylim, ylab='', xaxt='n', yaxt='n')
  axis(side=2, at=c(-100, 0, 100))
  par(mar=c(5,1,0,0.2))
  plot((offset + 1:1000)/200, xtrace$V1[offset + 1:1000] - xtrace$V2[offset + 1:1000], type='l', ylim=ylim, ylab='', yaxt='n',
       xlab="Time (s)")
  axis(side=2, at=c(-100, 0, 100))

  dev.off()
}

errorChart = function(offset) {
  startPdf()
  plot((offset + 1:1000)/200, xtrace$V1[offset + 1:1000] - xtrace$V2[offset + 1:1000], type='l', xlab="Time (s)", ylab="mV", ylim=ylim,
       main=paste(offset/200, " to ", (offset+1000)/200, "seconds"), yaxt='n')
  axis(side=2, at=c(-100, 0, 100))
  dev.off()
}


originalChart = function(offset, size=1000) {
  startPdf()
  plot((offset + 1:size)/200, xtrace$V1[offset + 1:size], type='l', xlab="Time (s)", ylab="mV", ylim=ylim,
       main=paste(offset/200, " to ", (offset+size)/200, "seconds"), yaxt='n')
  axis(side=2, at=c(-100, 0, 100))
  dev.off()
}



startPdf = function () {
  pdf(sprintf("figure-%02d.pdf", figure), width=6, height=4, pointsize=11)
  assign("figure", figure + 1, envir = .GlobalEnv)
}


# these show normal behavior
threePlot(0)
errorChart(0)
errorChart(1000)
errorChart(2000)

# reconstruction error is clearly not normally distributed
error = (xtrace$V1[1:20000]-xtrace$V2[1:20000])
startPdf()
qqnorm(error, cex=0.3, main="Reconstruction error is not normally distributed")
dev.off()

# but it is very tightly constrained
startPdf()
hist(error, breaks=70, main = "Reconstruction Error", xlab="Error (mV)", freq=F)
text(-55, 0.09, bquote(sigma == .(sprintf("%.1f", sd(error)))), adj=c(0,0))
text(-55, 0.08, sprintf("P(error > %.0f) = %.1f%%", 3*sd(error), 100*mean(abs(error) > 3 * sd(error))), adj=c(0,0))
text(-55, 0.07, sprintf("P(error > 100) < %.0f ppm", 1e6/20000), adj=c(0,0))
dev.off()

# at 100 seconds in, we see an anomaly
errorChart(20000)
threePlot(20000)
originalChart(20200, 200)

errorChart(48000)
threePlot(48000)


startPdf()
layout(matrix(1:64, ncol=8))
par(mar=c(0,0,0,0))
ix = order(-table(xtrace$V3))
for (i in ix[1:64]) {
  ymax = max(abs(dict[i,]))
  plot(t(dict[i,]), type='l', xaxt='n', yaxt='n', ylim=c(-ymax,ymax))
}
dev.off()
