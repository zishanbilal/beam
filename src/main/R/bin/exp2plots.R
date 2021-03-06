#!/usr/local/bin/Rscript
##############################################################################################################################################
# Script to process results of a BEAM experiment and create some default plots comparing the runs against each other by the 
# factors. This is intended to be run from the project root directory.
# 
# Argument: the path to the experiment directory containing the .yaml file defining the experiment *and* the runs directory containing the 
# results.
##############################################################################################################################################
setwd('/Users/critter/Dropbox/ucb/vto/beam-all/beam') # for development and debugging
source('./src/main/R/beam-utilities.R')
load.libraries(c('optparse'),quietly=T)
load.libraries(c('maptools','sp','stringr','ggplot2'))

##############################################################################################################################################
# COMMAND LINE OPTIONS 
option_list <- list(
)
if(interactive()){
  #setwd('~/downs/')
  #args<-'/Users/critter/Documents/beam/beam-output/experiments/vot'
  #args<-'/Users/critter/Documents/beam/beam-output/experiments/transit-price'
  args<-'/Users/critter/Documents/beam/beam-output/experiments/ridehail-price'
  #args<-'/Users/critter/Documents/beam/beam-output/experiments/transit-capacity'
  #args<-'/Users/critter/Documents/beam/beam-output/experiments/ridehail-capacity'
  args<-'/Users/critter/Documents/beam/beam-output/experiments/pruning'
  #args<-'/Users/critter/Documents/beam/beam-output/experiments/prices-25k/'
  args <- parse_args(OptionParser(option_list = option_list,usage = "exp2plots.R [experiment-directory]"),positional_arguments=T,args=args)
}else{
  args <- parse_args(OptionParser(option_list = option_list,usage = "exp2plots.R [experiment-directory]"),positional_arguments=T)
}
######################################################################################################

factor.to.scale.personal.back <- 20
factor.to.scale.transit.back <- 2
plot.congestion <- F

######################################################################################################
# Load the exp config
exp.dir <- ifelse(strtail(args$args)=="/",args$args,pp(args$args,"/"))
exp.file <- pp(exp.dir,'runs/experiments.csv')
plots.dir <- pp(exp.dir,'plots/')
make.dir(plots.dir)
exp <- data.table(read.csv(exp.file))
factors <- as.character(sapply(sapply(str_split(exp$experimentalGroup[1],"__")[[1]],str_split,"_"),function(x){ x[1] }))

levels <- list()
for(fact in factors){
  levels[[fact]] <- streval(pp('u(exp$',fact,')'))
}

grp <- exp$experimentalGroup[1]
evs <- list()
links <- list()
for(run.i in 1:nrow(exp)){
  grp <-  exp$experimentalGroup[run.i]
  run.dir <- pp(exp.dir,'runs/run.',grp,'/')
  events.csv <- pp(run.dir,'output/ITERS/it.0/0.events.csv')
  tt.csv <- pp(run.dir,'output/ITERS/it.0/0.linkstats.txt.gz')
  ev <- csv2rdata(events.csv)
  ev[,run:=grp]
  for(fact in factors){
    if(fact %in% names(ev))stop(pp('Factor name "',fact,'" also a column name in events.csv, please change factor name in experiments.csv'))
    the.level <- streval(pp('exp$',fact,'[run.i]'))
    streval(pp('ev[,',fact,':="',the.level,'"]'))
  }
  evs[[length(evs)+1]] <- ev[type%in%c('PathTraversal','ModeChoice')]
  if(plot.congestion){
    link <- parse.link.stats(tt.csv)
    streval(pp('link[,',fact,':="',the.level,'"]'))
    for(fact in factors){
      the.level <- streval(pp('exp$',fact,'[run.i]'))
      streval(pp('link[,',fact,':="',the.level,'"]'))
    }
    links[[length(links)+1]] <- link
  }
}
ev <- rbindlist(evs,use.names=T,fill=T)
rm('evs')
if(plot.congestion){
  link <- rbindlist(links,use.names=T,fill=T)
  rm('links')
}

ev <- clean.and.relabel(ev,factor.to.scale.personal.back)

setkey(ev,type)

## Prep data needed to do quick version of energy calc
en <- data.table(read.csv('~/Dropbox/ucb/vto/beam-all/beam/test/input/sf-light/energy/energy-consumption.csv'))
setkey(en,vehicleType)
en <- u(en)
## Energy Density in MJ/liter or MJ/kWh
en.density <- data.table(fuelType=c('gasoline','diesel','electricity'),density=c(34.2,35.8,3.6))
ev[tripmode%in%c('car') & vehicle_type=='Car',':='(num_passengers=1)]
ev[,pmt:=num_passengers*length/1609]
ev[is.na(pmt),pmt:=0]

scale_fill_manual(values = colours)


#########################
# Mode Choice Plots
#########################
mc <- ev[J('ModeChoice')]
setkey(mc,run,time)
mc[,tripIndex:=1:length(time),by=c('run','person','tourIndex')]
for(fact in factors){
  streval(pp('mc[,the.factor:=',fact,']'))
  if(all(c('low','base','high') %in% u(ev$the.factor))){
    mc[,the.factor:=factor(the.factor,levels=c('low','base','high'))]
  }else if(all(c('Low','Base','High') %in% u(mc$the.factor))){
    mc[,the.factor:=factor(the.factor,levels=c('Low','Base','High'))]
  }else{
    streval(pp('mc[,the.factor:=factor(the.factor,levels=exp$',fact,')]'))
  }
  
  # Modal splits 
  toplot <- mc[tripIndex==1,.(tot=length(time)),by=the.factor]
  toplot <- join.on(mc[tripIndex==1,.(num=length(time)),by=c('the.factor','tripmode')],toplot,'the.factor','the.factor')
  toplot[,frac:=num/tot]
  toplot[,tripmode:=pretty.modes(tripmode)]
  setkey(toplot,the.factor,tripmode)
  p <- ggplot(toplot,aes(x=the.factor,y=frac*100,fill=tripmode))+geom_bar(stat='identity',position='stack')+labs(x="Scenario",y="% of Trips",title=pp('Factor: ',fact),fill="Trip Mode")+
      scale_fill_manual(values=as.character(mode.colors$color.hex[match(sort(u(toplot$tripmode)),mode.colors$key)]))
  pdf.scale <- .6
  ggsave(pp(plots.dir,'mode-split-by-',fact,'.pdf'),p,width=10*pdf.scale,height=6*pdf.scale,units='in')

  target <- data.frame(tripmode=rep(c('Car','Walk','Transit','TNC'),length(u(toplot$the.factor))),
                       perc=rep(c(79,4,13,5),length(u(toplot$the.factor))),
                       the.factor=rep(u(toplot$the.factor),each=4))
  p <- ggplot(toplot,aes(x=tripmode,y=frac*100))+geom_bar(stat='identity')+facet_wrap(~the.factor)+geom_point(data=target,aes(y=perc),colour='red')
  ggsave(pp(plots.dir,'mode-split-lines-by-',fact,'.pdf'),p,width=15*pdf.scale,height=8*pdf.scale,units='in')

  # Accessibility
  pdf.scale <- .8
  p <- ggplot(mc[tripIndex==1,.(access=mean(access,na.rm=T)),by=c('the.factor','personalVehicleAvailable')],aes(x=the.factor,y=access))+geom_bar(stat='identity')+facet_wrap(~personalVehicleAvailable)+labs(x=fact,y="Avg. Accessibility Score",title='Accessibility by Availability of Private Car')
  ggsave(pp(plots.dir,'accessibility-by-private-vehicle.pdf'),p,width=10*pdf.scale,height=8*pdf.scale,units='in')
  p <- ggplot(mc[tripIndex==1,.(access=mean(access,na.rm=T)),by=c('the.factor','mode')],aes(x=the.factor,y=access))+geom_bar(stat='identity')+facet_wrap(~mode)+labs(x=fact,y="Avg. Accessibility Score",title='Accessibility by Chosen Mode')
  ggsave(pp(plots.dir,'accessibility-by-chosen-mode.pdf'),p,width=10*pdf.scale,height=8*pdf.scale,units='in')
  p <- ggplot(mc[tripIndex==1,.(access=mean(access,na.rm=T)),by=c('the.factor')],aes(x=the.factor,y=access))+geom_bar(stat='identity')+labs(x=fact,y="Avg. Accessibility Score",title='Overall Accessibility')
  ggsave(pp(plots.dir,'accessibility.pdf'),p,width=10*pdf.scale,height=8*pdf.scale,units='in')
}
rm('mc')


#########################
# Path Traversal Plots
#########################
pt <- ev[J('PathTraversal')]
for(fact in factors){
  # Energy by Mode
  streval(pp('pt[,the.factor:=',fact,']'))
  if(all(c('low','base','high') %in% u(ev$the.factor))){
    pt[,the.factor:=factor(the.factor,levels=c('low','base','high'))]
  }else if(all(c('Low','Base','High') %in% u(ev$the.factor))){
    pt[,the.factor:=factor(the.factor,levels=c('Low','Base','High'))]
  }else{
    streval(pp('pt[,the.factor:=factor(the.factor,levels=exp$',fact,')]'))
  }

  toplot <- pt[,.(fuel=sum(fuel),numVehicles=as.double(length(fuel)),numberOfPassengers=as.double(sum(num_passengers)),pmt=sum(pmt)),by=c('the.factor','vehicle_type','tripmode')]
  toplot <- toplot[vehicle_type!='Human' & tripmode!="walk"]
  if(nrow(en)>30){
    toplot <- join.on(toplot,en[vehicle%in%c('SUBWAY-DEFAULT','BUS-DEFAULT','CABLE_CAR-DEFAULT','CAR','FERRY-DEFAULT','TRAM-DEFAULT','RAIL-DEFAULT')|vehicleType=='TNC'],'vehicle_type','vehicleType','fuelType')
  }else{
    toplot <- join.on(toplot,en,'vehicle_type','vehicleType','fuelType')
  }
  toplot <- join.on(toplot,en.density,'fuelType','fuelType')
  toplot[,energy:=fuel*density]
  toplot[vehicle_type=='TNC',tripmode:='TNC']
  toplot[vehicle_type%in%c('Car','TNC'),energy:=energy*factor.to.scale.personal.back]
  toplot[vehicle_type%in%c('Car','TNC'),numVehicles:=numVehicles*factor.to.scale.personal.back]
  toplot[vehicle_type%in%c('Car','TNC'),pmt:=pmt*factor.to.scale.personal.back]
  toplot[vehicle_type%in%c('Car','TNC'),numberOfPassengers:=numVehicles]
  toplot[,ag.mode:=tripmode]
  toplot[tolower(ag.mode)%in%c('bart','bus','cable_car','muni','rail','tram','transit'),ag.mode:='Transit']
  toplot[ag.mode=='car',ag.mode:='Car']
  toplot.ag <- toplot[,.(energy=sum(energy),pmt=sum(pmt)),by=c('the.factor','ag.mode')]
  pdf.scale <- .6
  setkey(toplot.ag,the.factor,ag.mode)
  p <- ggplot(toplot.ag,aes(x=the.factor,y=energy/1e6,fill=ag.mode))+geom_bar(stat='identity',position='stack')+labs(x="Scenario",y="Energy Consumption (TJ)",title=to.title(fact),fill="Trip Mode")+
      scale_fill_manual(values=as.character(mode.colors$color.hex[match(sort(u(toplot.ag$ag.mode)),mode.colors$key)]))
  ggsave(pp(plots.dir,'energy-by-mode-',fact,'.pdf'),p,width=10*pdf.scale,height=6*pdf.scale,units='in')

  per.pmt <- toplot[,.(energy=sum(energy)/sum(pmt)),by=c('the.factor','tripmode')]
  per.pmt[,tripmode:=pretty.modes(tripmode)]
  the.first <- per.pmt[the.factor==per.pmt$the.factor[1]]
  per.pmt[,tripmode:=factor(tripmode,levels=the.first$tripmode[rev(order(the.first$energy))])]
  p <- ggplot(per.pmt[energy<Inf],aes(x=the.factor,y=energy,fill=tripmode))+geom_bar(stat='identity',position='dodge')+labs(x="Scenario",y="Energy Consumption (MJ/passenger mile)",title=to.title(fact),fill="Trip Mode")+
      scale_fill_manual(values=as.character(mode.colors$color.hex[match(levels(per.pmt$tripmode),mode.colors$key)]))
  ggsave(pp(plots.dir,'energy-per-pmt-by-vehicle-type-',fact,'.pdf'),p,width=10*pdf.scale,height=6*pdf.scale,units='in')

  # Deadheading 
  toplot <- pt[vehicle_type=='TNC',.(dead=num_passengers==0,miles=length/1609,hr,the.factor)]
  setkey(toplot,hr,dead)
  dead.frac <- toplot[,.(dead.frac=pp(roundC(100*sum(miles[dead==T])/sum(miles[dead==F]),1),"% Empty")),by=c('the.factor')]
  toplot <- toplot[,.(miles=sum(miles)),by=c('dead','hr','the.factor')]
  p <- ggplot(toplot,aes(x=hr,y=miles,fill=dead))+geom_bar(stat='identity')+labs(x="Hour",y="Vehicle Miles Traveled",fill="Empty",title=pp("TNC Deadheading"))+geom_text(data=dead.frac,hjust=1,aes(x=24,y=max(toplot$miles),label=dead.frac,fill=NA))+facet_wrap(~the.factor)
  pdf.scale <- .6
  ggsave(pp(plots.dir,'dead-heading.pdf'),p,width=10*pdf.scale,height=6*pdf.scale,units='in')
}
rm('pt')

#########################
# Network Plots
#########################
if(plot.congestion){
  for(fact in factors){
    streval(pp('link[,the.factor:=',fact,']'))
    link[,relative.sp:=(LENGTH/FREESPEED)/traveltime]
    congestion.levels <- c(1,0.5,0.1,0.01)
    congestion <- link[,.(level=congestion.levels,frac.below=sapply(congestion.levels,function(lev){ sum(relative.sp<lev)/length(relative.sp) })),by=c('hour','the.factor')]
    p <- ggplot(congestion,aes(x=hour,y=frac.below,colour=factor(level)))+geom_line()+facet_wrap(~the.factor)+labs(x="Hour",y="Fraction of Links Below Each Relative Speed",colour="Relative Speed",title=pp("Fraction of Links at Varying Levels of Congestion by ",fact))
    pdf.scale <- .6
    ggsave(pp(plots.dir,'congestion.pdf'),p,width=12*pdf.scale,height=8*pdf.scale,units='in')
  }
}

