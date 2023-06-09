package wtune.common.datasource.db;

import java.util.Arrays;
import java.util.List;

public final class sqlserverRules {
    public static final List<String> logicalRules = Arrays.asList(
            "AppIdxToApp",
            "ApplyHandler",
            "ApplyUAtoUniSJ",
            "CollapseSelects",
            "CommLASJN",
            "CommLOJN",
            "CommLSJN",
            "ExpandDistinctGbAgg",
            "ExpandInsertCons",
            "ExpandNAryJoinNoSnowflake",
            "FetchToApply",
            "GbAggAfterJoin",
            "GbAggBeforeJoin",
            "GbAggBeforeLOJ",
            "GbAggJNtoLSJN",
            "GbAggOnRestrRemap",
            "GbAggSmpfyEmptyT",
            "GbAggToConstScanOrTop",
            "GbAggToHS",
            "GbAggToPrj",
            "GbAggToSort",
            "GenLGAgg",
            "GetIdxToRng",
            "GetToIdxScan",
            "GetToTrivialScan",
            "IdxJNtoHS",
            "IJOnEmpty",
            "IJtoIJSEL",
            "ImpliedPredInnerAndAllLeftJn",
            "ImplRestrRemap",
            "InsertToStreamUpdate",
            "JNtoHS",
            "JNtoIdxLookup",
            "JNtoSM",
            "JoinCommute",
            "JoinLinearize",
            "JoinOnGbAgg",
            "JoinPredNorm",
            "JoinSwitch",
            "LASJNtoApply",
            "LASJNtoHS",
            "LASJNtoLASJNonDist",
            "LASJNtoSM",
            "LeftSideJNtoIdxLookup",
            "LogSTVFToPhySTVF",
            "LOJNtoApply",
            "LOJNtoHS",
            "LOJNtoSM",
            "LOJOnEmpty",
            "LOJtoLOJSEL",
            "LSJNtoApply",
            "LSJNtoHS",
            "LSJNtoJNonDist",
            "LSJNtoSM",
            "LSJOnEmpty",
            "LSJOnLclDist",
            "LSJtoLSJSEL",
            "OJOJSwitch",
            "PrjOnConstTbl",
            "PullApplyOverJoin",
            "PullLASJOverJoin",
            "PullLSJOverJoin",
            "RASJNtoHS",
            "RASJNtoSM",
            "ReduceGbAggExpr",
            "ReducePrjExpr",
            "RedundantLOJN",
            "ReorderLOJN",
            "ROJNtoHS",
            "ROJNtoSM",
            "RSJNtoHS",
            "RSJNtoSM",
            "SelectOnEmpty",
            "SelIdxToRng",
            "SELonJN",
            "SELonLOJ",
            "SELonNaryJoin",
            "SelOnPrj",
            "SELonSJ",
            "SELonTrue",
            "SELonUNIA",
            "SelPredNorm",
            "SelResToFilter",
            "SelToIdxStrategy",
            "SelToIndexOnTheFly",
            "SelToTrivialFilter",
            "SimplifyJoinWithCTG",
            "SimplifyLOJN",
            "SplitSemiApplyUnionAll",
            "StarJoinToHashJoinsWithBitmap",
            "StarSelJoinToHashJoinsWithBitmap",
            "UNIAtoMERGE",
            "WCJNonSELtoIdxLookup");

    public static final List<String> allRules = Arrays.asList(
            "JNtoNL"
            ,"LOJNtoNL"
            ,"LSJNtoNL"
            ,"LASJNtoNL"
            ,"JNtoSM"
            ,"FOJNtoSM"
            ,"LOJNtoSM"
            ,"ROJNtoSM"
            ,"LSJNtoSM"
            ,"RSJNtoSM"
            ,"LASJNtoSM"
            ,"RASJNtoSM"
            ,"IdxJNtoSM"
            ,"FOJnoneqToSM"
            ,"JNtoHS"
            ,"FOJNtoHS"
            ,"LOJNtoHS"
            ,"ROJNtoHS"
            ,"LSJNtoHS"
            ,"RSJNtoHS"
            ,"LASJNtoHS"
            ,"RASJNtoHS"
            ,"IdxJNtoHS"
            ,"PSJNtoHS"
            ,"HJwBMtoHS"
            ,"JoinCommute"
            ,"JoinSwitch"
            ,"JoinLinearize"
            ,"JoinLeftAssociate"
            ,"JoinRightAssociate"
            ,"JoinExchange"
            ,"ColocatedJoin"
            ,"StarJoinToIdxStrategy"
            ,"StarSelJoinToIdxStrategy"
            ,"StarJoinToHashJoinsWithBitmap"
            ,"StarSelJoinToHashJoinsWithBitmap"
            ,"GbTopToGbAgg"
            ,"GbTopAfterJoin"
            ,"GenGbApplySimple"
            ,"GenKeyBatch"
            ,"GenGbApplyAgg"
            ,"GbApplyAfterJoin"
            ,"CommFOJN"
            ,"CommLOJN"
            ,"CommLSJN"
            ,"CommLASJN"
            ,"CommROJN"
            ,"CommRSJN"
            ,"CommRASJN"
            ,"CommPSJN"
            ,"JoinOJSwitch"
            ,"OJJoinSwitch"
            ,"SelOJJoinSwitch"
            ,"OJOJSwitch"
            ,"SelOJOJSwitch"
            ,"ReorderLOJN"
            ,"LSJNtoDistOnJN"
            ,"LSJNtoJNonDist"
            ,"SubLSJNtoJNonDist"
            ,"LASJNtoLASJNonDist"
            ,"LSJOnLclDist"
            ,"LASJOnLclDist"
            ,"FOJNtoLSJNandLASJN"
            ,"DiscardLSJN"
            ,"DiscardRSJN"
            ,"PullJoinAboveLSJN"
            ,"PullJoinAboveLASJN"
            ,"PullJoinAboveApply"
            ,"PullJoinAboveSelApply"
            ,"JNtoJNSELNotNull"
            ,"JNonPrjLeft"
            ,"JNonPrjRight"
            ,"RedundantLOJN"
            ,"RedundantROJN"
            ,"RedundantApplyOJ"
            ,"SimplifyJoinWithCTG"
            ,"JoinWithCTGToSel"
            ,"ExpandDistinctGbAgg"
            ,"ReduceGbAgg"
            ,"GbAggWithNoReqdCol"
            ,"NormalizeGbAgg"
            ,"NormalizeTop"
            ,"GbAggOnPrj"
            ,"GbAggOnRestrRemap"
            ,"SelOnGbAgg"
            ,"SelOnSeqPrj"
            ,"GbAggToPrj"
            ,"GbAggSmpfyEmptyT"
            ,"ReduceGbAggExpr"
            ,"GbAggToStrm"
            ,"GbAggToHS"
            ,"GbAggToSort"
            ,"GbAggToUde"
            ,"GbAggAfterJoin"
            ,"GbAggAfterJoinSel"
            ,"GbAggAfterLOJ"
            ,"GbAggBeforeJoin"
            ,"GbAggUnderTopBeforeJoin"
            ,"GbAggUnderTopJoinBeforeJoin"
            ,"GbAggBeforeLOJ"
            ,"ScalarGbAggToTop"
            ,"GbAggToConstScanOrTop"
            ,"GbAggJNtoLSJN"
            ,"GbAggIFFPredicateUnderJoin"
            ,"GenLGAgg"
            ,"GenLGTop"
            ,"ReduceForDistinctAggs"
            ,"LocalAggBelowJoin"
            ,"LocalAggBelowPrjJoin"
            ,"LocalAggBelowLOJ"
            ,"LocalAggBelowFOJ"
            ,"PushLocalAgg"
            ,"LocalAggBelowUniAll"
            ,"GbAggBelowUniAll"
            ,"JoinOnGbAgg"
            ,"RmtEmptyVectorAgg"
            ,"DiscardPrj"
            ,"ReducePrjExpr"
            ,"SelOnPrj"
            ,"TOpOnPrj"
            ,"PrjOnConstTbl"
            ,"AddCompSca"
            ,"AddCCPrjToGet"
            ,"AddCCPrjToInsert"
            ,"AddCCPrjToUpdate"
            ,"AddCCPrjToDelete"
            ,"SelPredNorm"
            ,"JoinPredNorm"
            ,"ImpliedPredInnerAndAllLeftJn"
            ,"SimplifyMultiColumnJoinPred"
            ,"EnforceSort"
            ,"OrderByToNOP"
            ,"OrderByToSortInPlace"
            ,"SELonUNI"
            ,"SELonUNIA"
            ,"ReduceUnionCol"
            ,"ReduceUnionAllCol"
            ,"ReduceSwitchUnionCol"
            ,"UNIAtoCON"
            ,"SWUtoSWU"
            ,"UNIAtoMERGE"
            ,"UNItoDISonUNIA"
            ,"UNItoMERGE"
            ,"UNItoHASH"
            ,"CollapseUNI"
            ,"CollapseUNIA"
            ,"JNonUNIA"
            ,"LOJonUNIA"
            ,"LSJonUNIA"
            ,"LASJonUNIA"
            ,"CorrelateJNonUNIA"
            ,"JNonUNIAUNIA"
            ,"LOJonUNIAUNIA"
            ,"LSJonUNIAUNIA"
            ,"LASJonUNIAUNIA"
            ,"UNIAReorderInputs"
            ,"SplitUniIntoDistCompat"
            ,"SelOnRestrRemap"
            ,"SELonJN"
            ,"CleanupNaryJoin"
            ,"SELonNaryJoin"
            ,"SELonLOJ"
            ,"SELonROJ"
            ,"SELonSJ"
            ,"SELonApply"
            ,"SimplifyLOJN"
            ,"SimplifyROJN"
            ,"SimplifyFOJN"
            ,"SelOnTFP"
            ,"ApplyToNL"
            ,"ApplyCnstToNL"
            ,"RemoveSubqInSel"
            ,"RemoveSubqInPrj"
            ,"RemoveSubqInGbAgg"
            ,"RemoveSubqInJN"
            ,"RemoveSubqInLOJN"
            ,"RemoveSubqInLSJN"
            ,"RemoveSubqInLASJN"
            ,"RemoveSubqInFOJN"
            ,"RemoveSubqInTop"
            ,"RemoveSubqInSwitchUN"
            ,"RemoveSubqInTVF"
            ,"RemoveSubqInSTVF"
            ,"FOJNtoUnionAll"
            ,"ApplyHandler"
            ,"SelApplyHandler"
            ,"PrjApplyHandler"
            ,"ApplyUAtoUniSJ"
            ,"LSJNtoApply"
            ,"LASJNtoApply"
            ,"LOJNtoApply"
            ,"JNtoApplySTVF"
            ,"LOJPrjGetToApply"
            ,"LOJSelPrjGetToApply"
            ,"SplitLASJN"
            ,"SplitApplyLASJN"
            ,"LSJNtoAgg"
            ,"LASJNtoAgg"
            ,"PushApplyBelowJN"
            ,"PullApplyOverJoin"
            ,"PullSelApplyOverJoin"
            ,"PullLSJOverJoin"
            ,"PullLASJOverJoin"
            ,"PullColumnRestrPrjOverJoin"
            ,"PullColumnRestrPrjOverJoinSel"
            ,"LASJDistincttoLASJ"
            ,"LSJDistincttoLSJ"
            ,"PushTFPBelowJoin"
            ,"GetToScan"
            ,"GetToAssert"
            ,"SelectToFilter"
            ,"ConstGetToConstScan"
            ,"ImplRestrRemap"
            ,"ProjectToComputeScalar"
            ,"ReduceSequenceProjectExpr"
            ,"ImplementSequenceProject"
            ,"SplitSequenceProject"
            ,"EnforceBatch"
            ,"EnforceRow"
            ,"FetchToApply"
            ,"SelToIdxStrategy"
            ,"SelSTVFToSTVF"
            ,"SelSeqPrjToTop"
            ,"SelSeqPrjToAnyAgg"
            ,"SelToTrivialFilter"
            ,"GetToTrivialScan"
            ,"SelPrjGetToTrivialScan"
            ,"GetIdxToRng"
            ,"GetToIdxScan"
            ,"SelResToFilter"
            ,"AppIdxToApp"
            ,"SelIdxToRng"
            ,"CrsFtchToUnionAll"
            ,"JNtoIdxLookup"
            ,"LeftSideJNtoIdxLookup"
            ,"PSJNtoIdxLookup"
            ,"WCJNonSELtoIdxLookup"
            ,"PSJNonSELtoIdxLookup"
            ,"SelToIndexOnTheFly"
            ,"JoinToIndexOnTheFly"
            ,"SelIterToIdxOnFly"
            ,"JoinIterToIdxOnFly"
            ,"SelOnFetch"
            ,"JNFetchGetToApply"
            ,"JNSelFetchGetToApply"
            ,"RemoveViewAnchor"
            ,"AnyOnEmptyTrivial"
            ,"RmtUpdateOnEmpty"
            ,"RmtDeleteOnEmpty"
            ,"RmtInsertOnEmpty"
            ,"SelectOnEmpty"
            ,"GbAggOnEmpty"
            ,"RollupOnEmpty"
            ,"CubeOnEmpty"
            ,"PrjOnEmpty"
            ,"SpoolOnEmpty"
            ,"UpdateOnEmpty"
            ,"DeleteOnEmpty"
            ,"InsertOnEmpty"
            ,"ApplyOnEmpty"
            ,"NaryJoinOnEmpty"
            ,"IJOnEmpty"
            ,"LSJOnEmpty"
            ,"RSJOnEmpty"
            ,"LASJOnEmpty"
            ,"RASJOnEmpty"
            ,"LOJOnEmpty"
            ,"ROJOnEmpty"
            ,"FOJOnEmpty"
            ,"LASJOnEmptyRight"
            ,"RASJOnEmptyLeft"
            ,"LOJOnEmptyRight"
            ,"ROJOnEmptyLeft"
            ,"FOJOnOneEmpty"
            ,"SELonTrue"
            ,"TopOnEmpty"
            ,"DiscardTop"
            ,"EmptyIterator"
            ,"SelToLSJ"
            ,"SelToLASJ"
            ,"ImplementFastFwd"
            ,"ImplementFastFwdAsInsert"
            ,"EnforceHPandAccCard"
            ,"BuildSpool"
            ,"AddNOPToCSRootSpool"
            ,"ScrollLockAsFetch"
            ,"ScrollLockAsDynamic"
            ,"SpoolGetToGet"
            ,"UpdateToStreamUpdate"
            ,"DeleteToStreamUpdate"
            ,"InsertToStreamUpdate"
            ,"PutToPhysicalPut"
            ,"ExpandUpdateCons"
            ,"ExpandDeleteCons"
            ,"ExpandInsertCons"
            ,"ExpandUpdateImplicitAssignments"
            ,"ExpandInsertImplicitAssignments"
            ,"ExpandInsertToPut"
            ,"ExpandPtnViewIns"
            ,"ExpandPtnViewUpd"
            ,"ExpandPtnViewDel"
            ,"AssertToStreamCheck"
            ,"ExpandVerifyCnst"
            ,"BuildSplit"
            ,"BuildCollapse"
            ,"BuildSequence"
            ,"IndexCreate"
            ,"IndexCreateOnPrj"
            ,"SplitDictionaryBuild"
            ,"ExpandInsteadOfTriggerIns"
            ,"ExpandInsteadOfTriggerUpd"
            ,"ExpandInsteadOfTriggerDel"
            ,"ColocatedInsert"
            ,"ResumableIndexBuild"
            ,"MatchAggGraphOp"
            ,"MatchJoin"
            ,"MatchPrjJoin"
            ,"MatchSelectGet"
            ,"MatchPrjSelectGet"
            ,"MatchGet"
            ,"MatchPrjGet"
            ,"CubeSimpleAgg"
            ,"RollupSimpleAgg"
            ,"CubeToRollup"
            ,"CubeNaive"
            ,"CubeGbAgg"
            ,"CubeNoGbAgg"
            ,"ReduceRollupExpr"
            ,"ReduceCubeExpr"
            ,"RollupGbAgg"
            ,"RollupToStrm"
            ,"BuildTop"
            ,"BuildGlobalTop"
            ,"BuildLocalTop"
            ,"TopRowcountGbPrj"
            ,"BuildGbTop"
            ,"BuildGbApply"
            ,"BuildKeyBatchApply"
            ,"BuildUde"
            ,"BuildBsl"
            ,"BuildTFP"
            ,"IJtoIJSEL"
            ,"LSJtoLSJSEL"
            ,"RSJtoRSJSEL"
            ,"LASJtoLASJSEL"
            ,"RASJtoRASJSEL"
            ,"LOJtoLOJSEL"
            ,"ROJtoROJSEL"
            ,"ApplyToSM"
            ,"ApplyCnstToSM"
            ,"ExpandNAryJoinNoSnowflake"
            ,"ExpandNAryJoinWithSnowflake"
            ,"GatherDistrCompatJoins"
            ,"ExpandNAryJoinToBatchSnowflake"
            ,"ExpandPivot"
            ,"ExpandUnpivot"
            ,"ExpandPivotLOJ"
            ,"SelOnPivot"
            ,"SpoolOnIterator"
            ,"IterateToDepthFirst"
            ,"RecRefToConstTble"
            ,"IteratorToAnchor"
            ,"SelOnIterator"
            ,"LogTVFToPhyTVF"
            ,"LogSTVFToPhySTVF"
            ,"STVFOrderCheck"
            ,"JoinToStarJoin"
            ,"JoinOnStarJoin"
            ,"JoinOnSelStarJoin"
            ,"GetToRmtScan"
            ,"SpoolOverRmtScan"
            ,"BuildRmtQuery"
            ,"SpoolOverRmtQuery"
            ,"LogRmtQToPhyRmtQ"
            ,"RmtInsertToRmtModify"
            ,"RmtUpdateToRmtModify"
            ,"RmtDeleteToRmtModify"
            ,"ParametrizeRmtUpdate"
            ,"ParametrizeRmtUpdateNoPrj"
            ,"ParametrizeRmtDelete"
            ,"ParametrizeRmtDeleteNoPrj"
            ,"GetIdxToRmtRange"
            ,"SelIdxToRmtRange"
            ,"FtchToRmtFtch"
            ,"RemoteHint"
            ,"JoinToApply"
            ,"NAryJoinToApply"
            ,"SplitSelects"
            ,"CollapseSelects"
            ,"TopOnRmtGet"
            ,"TopOnRmtQuery"
            ,"OffsetOnRmtGet"
            ,"OffsetOnRmtQuery"
            ,"SpatialIntersectFilterOverGridIndex"
            ,"JoinExtToPrimaryFilter"
            ,"JoinExtToPrimaryFilterOverSelect"
            ,"SpatialJointoApply"
            ,"SpatialNearestNeighbor"
            ,"SelSTVFToIdxOnFly"
            ,"SelSTVFToSTVFExp"
            ,"NormalizeSequenceProject"
            ,"BuildNOP"
            ,"BuildExternalComputation"
            ,"SpoolOverExtStrTable"
            ,"RefIntegrityMaintainer"
            ,"CollapseIdenticalScalarSubquery"
            ,"GetToExtExtractScan"
            ,"SelOnChoose"
            ,"GraphIterateToDepthFirst"
            ,"GraphIterateWithOneJoin"
            ,"GenLGSequenceProject"
            ,"LocalGlobalCube"
            ,"SinglePassCube"
            ,"SplitSemiApplyUnionAll"
            ,"XcsScan"
            ,"ExpCubeToRollup"
            ,"PutSemijoinUnderGbApply"
            ,"GraphIterateOnGraphIterator"
            ,"GbAggSplitToRanges"
            ,"SelOnGbAggSplitToRanges");
}
