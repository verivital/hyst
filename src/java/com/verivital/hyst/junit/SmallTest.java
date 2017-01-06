package com.verivital.hyst.junit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.passes.complex.ContinuizationPass;
import com.verivital.hyst.passes.complex.ContinuizationPass.IntervalTerm;
import com.verivital.hyst.printers.DReachPrinter.DReachExpressionPrinter;
import com.verivital.hyst.printers.FlowstarPrinter;
import com.verivital.hyst.printers.SimulinkStateflowPrinter;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Classification;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.Bind;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExNetworkComponent;

/**
 * Small tests dealing with the expression parser or range extractor. Things that don't require
 * loading a whole model.
 * 
 * @author Stanley Bak
 *
 */
public class SmallTest
{
	@Before
	public void setUpClass()
	{
		Expression.expressionPrinter = null;
	}

	@Test
	public void testFormatDecimal()
	{
		Assert.assertEquals(ToolPrinter.doubleToString(0.3), "0.3");

		Assert.assertEquals(ToolPrinter.doubleToString(10.3), "10.3");
	}

	@Test
	public void testComplexFlowExpression()
	{
		String s = "v_i = Ii_dn / C_i & v_p1 = -v_p1 / C_p1 * (1 / R_p2 + 1 / R_p3) + v_p / (C_p1 * R_p3) + Ip_dn / C_p1 & v_p = v_p1 / (C_p3 * R_p3) - v_p / (C_p3 * R_p3) & phi_v = v_i * K_i / N + v_p * K_p / N + 6.28 * f_0 / N & phi_ref = 6.28 * f_ref & t = 0";

		FormulaParser.parseFlow(s);
	}

	@Test
	public void testLongFlowExpression()
	{
		String s = "x1' == x136 &x2' == x137 &x3' == x138 &x4' == x139 &x5' == x140 &x6' == x141 &x7' == x142 &x8' == x143 &x9' == x144 &x10' == x145 &x11' == x146 &x12' == x147 &x13' == x148 &x14' == x149 &x15' == x150 &x16' == x151 &x17' == x152 &x18' == x153 &x19' == x154 &x20' == x155 &x21' == x156 &x22' == x157 &x23' == x158 &x24' == x159 &x25' == x160 &x26' == x161 &x27' == x162 &x28' == x163 &x29' == x164 &x30' == x165 &x31' == x166 &x32' == x167 &x33' == x168 &x34' == x169 &x35' == x170 &x36' == x171 &x37' == x172 &x38' == x173 &x39' == x174 &x40' == x175 &x41' == x176 &x42' == x177 &x43' == x178 &x44' == x179 &x45' == x180 &x46' == x181 &x47' == x182 &x48' == x183 &x49' == x184 &x50' == x185 &x51' == x186 &x52' == x187 &x53' == x188 &x54' == x189 &x55' == x190 &x56' == x191 &x57' == x192 &x58' == x193 &x59' == x194 &x60' == x195 &x61' == x196 &x62' == x197 &x63' == x198 &x64' == x199 &x65' == x200 &x66' == x201 &x67' == x202 &x68' == x203 &x69' == x204 &x70' == x205 &x71' == x206 &x72' == x207 &x73' == x208 &x74' == x209 &x75' == x210 &x76' == x211 &x77' == x212 &x78' == x213 &x79' == x214 &x80' == x215 &x81' == x216 &x82' == x217 &x83' == x218 &x84' == x219 &x85' == x220 &x86' == x221 &x87' == x222 &x88' == x223 &x89' == x224 &x90' == x225 &x91' == x226 &x92' == x227 &x93' == x228 &x94' == x229 &x95' == x230 &x96' == x231 &x97' == x232 &x98' == x233 &x99' == x234 &x100' == x235 &x101' == x236 &x102' == x237 &x103' == x238 &x104' == x239 &x105' == x240 &x106' == x241 &x107' == x242 &x108' == x243 &x109' == x244 &x110' == x245 &x111' == x246 &x112' == x247 &x113' == x248 &x114' == x249 &x115' == x250 &x116' == x251 &x117' == x252 &x118' == x253 &x119' == x254 &x120' == x255 &x121' == x256 &x122' == x257 &x123' == x258 &x124' == x259 &x125' == x260 &x126' == x261 &x127' == x262 &x128' == x263 &x129' == x264 &x130' == x265 &x131' == x266 &x132' == x267 &x133' == x268 &x134' == x269 &x135' == x270 &x136' == 7.0757e-7*u1 + 0.14128*u2 + 6.5392e-6*u3 - 0.3887*x1 - 0.0062346*x136 &x137' == - 0.50662*u1 - 0.00011938*u2 - 0.036277*u3 - 0.60078*x2 - 0.007751*x137 &x138' == - 1.1789e-10*u1 - 7.4209e-10*u2 - 1.936e-11*u3 - 1.9781*x3 - 0.014065*x138 &x139' == - 8.1482e-6*u1 - 0.011052*u2 - 1.7213e-6*u3 - 1.9785*x4 - 0.014066*x139 &x140' == - 2.8375e-6*u1 - 0.018*u2 - 3.9165e-6*u3 - 3.1943*x5 - 0.017872*x140 &x141' == - 0.43873*u1 - 0.000096277*u2 - 0.030023*u3 - 3.9682*x6 - 0.01992*x141 &x142' == 0.036495*u1 + 0.00002396*u2 + 0.079403*u3 - 5.2756*x7 - 0.022969*x142 &x143' == 6.5641e-6*u1 - 0.0016156*u2 + 0.000046056*u3 - 5.3176*x8 - 0.02306*x143 &x144' == - 0.014835*u1 - 0.000017813*u2 - 0.05711*u3 - 6.1969*x9 - 0.024894*x144 &x145' == 0.0067326*u2 - 0.00015398*u1 - 0.00012595*u3 - 6.2041*x10 - 0.024908*x145 &x146' == 0.0107*u1 + 8.4365e-6*u2 + 0.04406*u3 - 6.6376*x11 - 0.025763*x146 &x147' == 0.0063217*u2 - 0.000047189*u1 + 3.8116e-6*u3 - 6.643*x12 - 0.025774*x147 &x148' == 4.4819e-6*u1 + 0.084208*u2 + 7.8716e-6*u3 - 14.915*x13 - 0.03862*x148 &x149' == - 0.18348*u1 - 0.000021428*u2 - 0.018073*u3 - 15.321*x14 - 0.039142*x149 &x150' == - 0.00013119*u1 - 7.2263e-6*u2 - 0.00098969*u3 - 28.924*x15 - 0.053781*x150 &x151' == 0.000014322*u1 + 0.0011906*u2 + 2.9172e-7*u3 - 28.924*x16 - 0.053781*x151 &x152' == 0.000027621*u3 - 0.030751*u2 - 2.3504e-7*u1 - 31.36*x17 - 0.056*x152 &x153' == 0.000019166*u2 - 0.10195*u1 - 0.013061*u3 - 31.666*x18 - 0.056273*x153 &x154' == 0.000083625*u1 - 1.1436e-6*u2 + 0.00056631*u3 - 34.377*x19 - 0.058632*x154 &x155' == 5.876e-6*u1 - 0.00036539*u2 + 1.1914e-6*u3 - 34.377*x20 - 0.058632*x155 &x156' == 0.018605*u2 - 5.9252e-7*u1 - 0.000020965*u3 - 37.182*x21 - 0.060977*x156 &x157' == 0.037589*u1 - 0.000014814*u2 + 0.005498*u3 - 37.229*x22 - 0.061016*x157 &x158' == 0.00341*u2 - 0.068525*u1 - 0.46585*u3 - 62.94*x23 - 0.079335*x158 &x159' == - 0.000075636*u1 - 0.072935*u2 - 0.00025308*u3 - 65.885*x24 - 0.08117*x159 &x160' == 0.0001139*u2 - 0.018658*u1 - 0.029034*u3 - 65.946*x25 - 0.081207*x160 &x161' == 0.001394*u1 + 0.86515*u2 - 0.00025457*u3 - 71.925*x26 - 0.084809*x161 &x162' == 0.0018635*u2 - 0.099922*u1 + 0.48228*u3 - 84.946*x27 - 0.092166*x162 &x163' == 3.1501e-6*u1 - 0.0030873*u2 - 0.000090546*u3 - 85.054*x28 - 0.092225*x163 &x164' == 0.0022359*u2 - 0.14678*u1 + 0.60815*u3 - 85.262*x29 - 0.092337*x164 &x165' == 0.0022772*u2 - 1.116e-7*u1 + 0.000014395*u3 - 91.273*x30 - 0.095537*x165 &x166' == 0.012348*u1 - 0.00001569*u2 - 0.014482*u3 - 91.283*x31 - 0.095542*x166 &x167' == 4.1293e-7*u2 - 0.000025598*u1 + 0.000061057*u3 - 95.44*x32 - 0.097693*x167 &x168' == 7.9976e-8*u1 - 0.000022157*u2 - 2.6977e-7*u3 - 95.44*x33 - 0.097693*x168 &x169' == 0.000011174*u1 + 6.5102e-7*u2 - 0.000026667*u3 - 95.457*x34 - 0.097702*x169 &x170' == 1.1804e-6*u1 - 8.8569e-6*u2 - 2.0968e-6*u3 - 95.457*x35 - 0.097702*x170 &x171' == 3.0684e-6*u1 + 1.474e-6*u2 - 7.2696e-6*u3 - 95.492*x36 - 0.09772*x171 &x172' == 3.4433e-6*u2 - 1.3712e-6*u1 + 3.306e-6*u3 - 95.492*x37 - 0.09772*x172 &x173' == 0.02642*u2 - 0.000013063*u1 + 0.000069845*u3 - 105.24*x38 - 0.10258*x173 &x174' == 0.0065433*u3 - 0.000075167*u2 - 0.035277*u1 - 105.32*x39 - 0.10262*x174 &x175' == 0.00012439*u3 - 0.043783*u2 - 0.000018536*u1 - 115.82*x40 - 0.10762*x175 &x176' == 0.031319*u3 - 0.000099723*u2 - 0.081315*u1 - 116.34*x41 - 0.10786*x176 &x177' == 0.000092377*u2 - 0.0044785*u1 - 0.018757*u3 - 178.25*x42 - 0.13351*x177 &x178' == 0.000023088*u3 - 0.0043648*u2 - 5.1618e-6*u1 - 178.73*x43 - 0.13369*x178 &x179' == - 0.00042515*u1 - 0.064373*u2 - 0.00052336*u3 - 233.48*x44 - 0.1528*x179 &x180' == 0.000053608*u3 - 0.051906*u2 - 9.6891e-6*u1 - 305.2*x45 - 0.1747*x180 &x181' == 0.064984*u1 + 0.00021393*u2 - 0.0062467*u3 - 306.11*x46 - 0.17496*x181 &x182' == 0.004123*u1 - 0.00062444*u2 + 0.031733*u3 - 336.0*x47 - 0.1833*x182 &x183' == 0.010422*u2 - 0.000045821*u1 - 0.00010101*u3 - 338.12*x48 - 0.18388*x183 &x184' == 0.35854*u2 - 0.0018318*u1 - 0.01054*u3 - 437.91*x49 - 0.20926*x184 &x185' == 0.00020889*u2 - 0.020001*u1 + 0.0066287*u3 - 444.23*x50 - 0.21077*x185 &x186' == 0.45414*u2 - 0.006195*u1 - 0.50667*u3 - 467.28*x51 - 0.21617*x186 &x187' == - 0.0010414*u1 - 0.41616*u2 - 0.56217*u3 - 468.98*x52 - 0.21656*x187 &x188' == 0.000026893*u1 + 2.9602e-6*u2 - 0.003364*u3 - 505.12*x53 - 0.22475*x188 &x189' == 0.00010047*u1 - 0.013986*u2 - 0.00032624*u3 - 505.14*x54 - 0.22475*x189 &x190' == 0.00010351*u3 - 0.0071942*u2 - 0.0062849*u1 - 730.75*x55 - 0.27032*x190 &x191' == 0.0017199*u2 - 0.030556*u1 + 0.0012875*u3 - 730.76*x56 - 0.27033*x191 &x192' == 0.00088724*u3 - 0.00011461*u2 - 0.00011525*u1 - 749.99*x57 - 0.27386*x192 &x193' == 0.00013764*u1 + 0.00079088*u2 + 0.00015114*u3 - 749.99*x58 - 0.27386*x193 &x194' == 1.1803e-6*u1 - 0.00002544*u2 - 0.00006453*u3 - 831.1*x59 - 0.28829*x194 &x195' == 0.00043811*u1 - 9.8283e-6*u2 - 0.0033707*u3 - 831.1*x60 - 0.28829*x195 &x196' == 6.5479e-6*u1 - 0.000018367*u2 - 0.000025361*u3 - 908.85*x61 - 0.30147*x196 &x197' == 0.000065276*u2 - 0.0050359*u1 + 0.0014387*u3 - 909.06*x62 - 0.30151*x197 &x198' == 0.000096012*u2 - 0.0045682*u1 + 0.025285*u3 - 934.4*x63 - 0.30568*x198 &x199' == 0.00058978*u3 - 0.0011608*u2 - 0.000078889*u1 - 934.43*x64 - 0.30568*x199 &x200' == 0.00057634*u1 + 0.000036046*u2 - 0.0025508*u3 - 1013.9*x65 - 0.31842*x200 &x201' == 0.00017215*u1 - 0.0011353*u2 - 0.000087535*u3 - 1013.9*x66 - 0.31842*x201 &x202' == 9.0275e-6*u3 - 0.00027271*u2 - 0.000051705*u1 - 1089.9*x67 - 0.33013*x202 &x203' == 0.00002415*u2 - 0.0054866*u1 - 0.00003718*u3 - 1089.9*x68 - 0.33013*x203 &x204' == 0.000036797*u2 - 0.0054376*u1 + 0.00015172*u3 - 1122.1*x69 - 0.33498*x204 &x205' == 0.00031667*u1 + 0.00054782*u2 - 0.000018049*u3 - 1122.1*x70 - 0.33498*x205 &x206' == 3.0757e-6*u2 - 1.2617e-6*u1 + 1.1409e-6*u3 - 1147.7*x71 - 0.33878*x206 &x207' == 4.1452e-6*u1 - 0.000010395*u2 - 1.0082e-7*u3 - 1147.7*x72 - 0.33878*x207 &x208' == 3.1599e-6*u2 - 0.000022452*u1 + 0.000059826*u3 - 1147.9*x73 - 0.33881*x208 &x209' == 9.8304e-6*u1 - 0.000014815*u2 + 4.4949e-6*u3 - 1147.9*x74 - 0.33881*x209 &x210' == 0.000060459*u3 - 2.9022e-7*u2 - 0.000022195*u1 - 1148.2*x75 - 0.33886*x210 &x211' == 8.92e-6*u2 - 3.3025e-6*u1 - 5.3807e-8*u3 - 1148.2*x76 - 0.33886*x211 &x212' == 0.24221*u1 + 0.0015138*u2 + 0.014898*u3 - 1219.6*x77 - 0.34923*x212 &x213' == 0.000088813*u1 + 0.023045*u2 - 0.00020996*u3 - 1230.4*x78 - 0.35077*x213 &x214' == - 1.1952*u1 - 0.0063103*u2 - 0.081024*u3 - 1442.9*x79 - 0.37986*x214 &x215' == 0.0014984*u1 - 0.0072565*u2 + 0.00020512*u3 - 1532.3*x80 - 0.39144*x215 &x216' == 0.023607*u1 + 0.00018271*u2 + 0.0038478*u3 - 1532.6*x81 - 0.39148*x216 &x217' == 0.002905*u1 - 0.016819*u2 + 0.0003064*u3 - 1608.8*x82 - 0.4011*x217 &x218' == 0.011915*u3 - 0.00012966*u2 - 0.0136*u1 - 1609.1*x83 - 0.40114*x218 &x219' == 0.00015566*u1 - 0.018795*u2 + 0.0016063*u3 - 1752.0*x84 - 0.41857*x219 &x220' == 0.0010014*u2 - 0.021483*u1 + 0.015257*u3 - 1752.2*x85 - 0.41859*x220 &x221' == 0.0005643*u2 - 0.00031682*u1 + 0.000462*u3 - 1782.2*x86 - 0.42216*x221 &x222' == 0.00056935*u1 + 0.00031009*u2 - 0.00033678*u3 - 1782.2*x87 - 0.42216*x222 &x223' == 0.00058536*u2 - 0.0011199*u1 + 0.014129*u3 - 1808.4*x88 - 0.42525*x223 &x224' == 0.0005489*u1 + 0.00065079*u2 - 0.00031026*u3 - 1809.0*x89 - 0.42532*x224 &x225' == 4.9723e-6*u1 - 9.8472e-6*u2 + 2.6279e-6*u3 - 1846.1*x90 - 0.42967*x225 &x226' == 4.9101e-10*u1 - 1.1223e-9*u2 + 2.229e-10*u3 - 1846.1*x91 - 0.42967*x226 &x227' == 0.000049189*u1 - 0.00022687*u2 - 0.00021283*u3 - 1910.9*x92 - 0.43714*x227 &x228' == 0.00015051*u1 + 0.000051321*u2 - 0.00012872*u3 - 1910.9*x93 - 0.43714*x228 &x229' == 0.00063177*u1 + 0.00017696*u2 - 0.0010892*u3 - 1948.5*x94 - 0.44142*x229 &x230' == 0.0061807*u1 + 0.00032646*u2 - 0.0085195*u3 - 1948.5*x95 - 0.44142*x230 &x231' == 0.0012237*u1 + 0.0001993*u2 - 0.0021922*u3 - 1949.0*x96 - 0.44147*x231 &x232' == 0.0017744*u1 + 0.000016868*u2 - 0.0020571*u3 - 1949.0*x97 - 0.44147*x232 &x233' == 0.0099946*u3 - 0.011417*u2 - 0.0049765*u1 - 2050.7*x98 - 0.45285*x233 &x234' == 0.01233*u1 - 0.0035991*u2 - 0.022365*u3 - 2050.8*x99 - 0.45285*x234 &x235' == 0.01543*u1 + 0.0016228*u2 - 0.030353*u3 - 2091.3*x100 - 0.45731*x235 &x236' == 0.0042589*u2 - 0.00049031*u1 + 0.0006439*u3 - 2091.4*x101 - 0.45732*x236 &x237' == 0.020253*u1 + 0.0043904*u2 - 0.046049*u3 - 2180.6*x102 - 0.46697*x237 &x238' == 0.014846*u2 - 0.0010619*u1 + 0.0013241*u3 - 2180.8*x103 - 0.46699*x238 &x239' == 0.17315*u3 - 0.014033*u2 - 0.075296*u1 - 2191.0*x104 - 0.46808*x239 &x240' == 0.0020218*u1 - 0.039024*u2 - 0.0018452*u3 - 2196.9*x105 - 0.46871*x240 &x241' == 0.10389*u1 + 0.51887*u2 - 0.31624*u3 - 2288.2*x106 - 0.47835*x241 &x242' == 0.32778*u2 - 0.19346*u1 + 0.50735*u3 - 2302.1*x107 - 0.4798*x242 &x243' == 0.000085029*u2 - 0.000032977*u1 + 0.00008903*u3 - 2575.4*x108 - 0.50748*x243 &x244' == 0.0012964*u2 - 0.014882*u1 + 0.026467*u3 - 2576.1*x109 - 0.50756*x244 &x245' == 0.013625*u2 - 0.00027498*u1 + 0.00020698*u3 - 2757.5*x110 - 0.52512*x245 &x246' == 0.014075*u3 - 0.00032325*u2 - 0.0091551*u1 - 2758.1*x111 - 0.52518*x246 &x247' == 0.0034082*u2 - 0.0012731*u1 + 0.00084288*u3 - 2772.3*x112 - 0.52653*x247 &x248' == 0.00054552*u2 - 0.011332*u1 + 0.07711*u3 - 2797.8*x113 - 0.52894*x248 &x249' == - 1.0055e-7*u1 - 0.00028818*u2 - 0.00003986*u3 - 2926.6*x114 - 0.54098*x249 &x250' == 0.0055521*u1 - 0.00025119*u2 - 0.006561*u3 - 2926.8*x115 - 0.541*x250 &x251' == 0.092188*u1 + 0.00038024*u2 - 0.0082241*u3 - 3146.5*x116 - 0.56094*x251 &x252' == 0.0026713*u1 - 0.053336*u2 - 0.000708*u3 - 3150.8*x117 - 0.56132*x252 &x253' == 0.0027229*u2 - 0.00003975*u1 + 8.5966e-6*u3 - 3212.8*x118 - 0.56682*x253 &x254' == 0.0010552*u1 + 0.00010518*u2 + 0.011092*u3 - 3213.4*x119 - 0.56687*x254 &x255' == 0.0042997*u1 + 0.015695*u2 - 0.0027019*u3 - 3281.1*x120 - 0.57281*x255 &x256' == 0.000049178*u1 + 0.0067973*u2 - 0.000056598*u3 - 3291.2*x121 - 0.57369*x256 &x257' == 0.0051307*u1 - 0.000051325*u2 - 0.0051118*u3 - 3291.4*x122 - 0.5737*x257 &x258' == 1.2671e-6*u2 - 0.000026483*u1 + 0.000014289*u3 - 3427.6*x123 - 0.58546*x258 &x259' == 2.794e-6*u1 + 8.4549e-6*u2 - 1.9638e-6*u3 - 3427.6*x124 - 0.58546*x259 &x260' == 8.7619e-6*u1 - 1.1027e-7*u2 - 4.7226e-6*u3 - 3428.8*x125 - 0.58556*x260 &x261' == 6.7506e-7*u1 + 2.7178e-6*u2 - 2.7931e-7*u3 - 3428.8*x126 - 0.58556*x261 &x262' == 2.7709e-7*u2 - 0.000020209*u1 + 0.000010883*u3 - 3429.3*x127 - 0.5856*x262 &x263' == 1.7931e-6*u1 + 7.377e-6*u2 - 8.2538e-7*u3 - 3429.3*x128 - 0.5856*x263 &x264' == 4.9166e-7*u2 - 0.000023736*u1 + 0.000012401*u3 - 3448.2*x129 - 0.58721*x264 &x265' == 7.4256e-7*u3 - 6.7173e-6*u2 - 1.7903e-6*u1 - 3448.2*x130 - 0.58721*x265 &x266' == 0.000020305*u1 - 2.2688e-7*u2 - 0.000010594*u3 - 3449.0*x131 - 0.58729*x266 &x267' == 1.7888e-6*u1 + 7.6133e-6*u2 - 6.2278e-7*u3 - 3449.0*x132 - 0.58729*x267 &x268' == 7.4511e-8*u2 - 2.1437e-7*u1 + 1.108e-7*u3 - 3452.3*x133 - 0.58757*x268 &x269' == 1.9937e-8*u1 - 1.2608e-7*u2 - 9.1708e-8*u3 - 3452.3*x134 - 0.58757*x269 &x270' == 0.009357*u1 - 0.00015898*u2 - 0.028368*u3 - 3762.6*x135 - 0.6134*x270 &t' == 1";

		Expression e = FormulaParser.parseFlow(s);
		Assert.assertNotNull(e);
	}

	@Test
	public void testLongInvariantExpression()
	{
		String s = "t <= stoptime &y1 == 2.6237e-9*x136 - 0.0017679*x137 - 4.1142e-13*x138 - 2.8282e-8*x139 - 1.0142e-8*x140 - 0.0015306*x141 + 0.00012722*x142 + 2.3721e-8*x143 - 0.000051674*x144 - 5.3718e-7*x145 + 0.000037262*x146 - 1.6535e-7*x147 + 1.9132e-8*x148 - 0.00064188*x149 - 4.5456e-7*x150 + 4.9666e-8*x151 - 1.7187e-9*x152 - 0.00035519*x153 + 2.9018e-7*x154 + 2.0541e-8*x155 - 1.6726e-9*x156 + 0.0001309*x157 - 0.00025662*x158 - 2.8148e-7*x159 - 0.000065652*x160 + 4.9589e-6*x161 - 0.00035597*x162 + 1.2252e-8*x163 - 0.00052144*x164 - 6.216e-10*x165 + 0.000043305*x166 - 8.8693e-8*x167 + 2.715e-10*x168 + 3.8714e-8*x169 + 4.0926e-9*x170 + 1.0631e-8*x171 - 4.7488e-9*x172 - 4.7431e-8*x173 - 0.00012302*x174 - 8.8338e-8*x175 - 0.00029126*x176 - 0.000014764*x177 - 1.2283e-8*x178 - 1.4364e-6*x179 - 3.3588e-8*x180 + 0.00022523*x181 + 0.000014148*x182 - 1.2543e-7*x183 - 5.8108e-6*x184 - 0.000090095*x185 - 0.000019709*x186 - 2.6373e-6*x187 + 8.166e-8*x188 + 3.2707e-7*x189 - 0.000021495*x190 - 0.00010405*x191 - 3.4934e-7*x192 + 4.7814e-7*x193 + 1.9483e-9*x194 + 1.26e-6*x195 + 1.8567e-8*x196 - 0.000015828*x197 - 0.000013005*x198 - 2.3535e-7*x199 + 1.6256e-6*x200 + 5.516e-7*x201 - 1.4891e-7*x202 - 0.000017391*x203 - 0.000017384*x204 + 9.7787e-7*x205 - 3.9206e-9*x206 + 1.3296e-8*x207 - 6.3093e-8*x208 + 3.2525e-8*x209 - 6.199e-8*x210 - 1.0536e-8*x211 + 0.00065804*x212 + 7.3292e-7*x213 - 0.0033769*x214 + 4.4497e-6*x215 + 0.000056984*x216 + 8.3667e-6*x217 - 0.000052875*x218 - 9.4889e-8*x219 - 0.000067644*x220 - 9.7053e-7*x221 + 1.8448e-6*x222 - 2.0639e-6*x223 + 1.8016e-6*x224 + 1.2926e-8*x225 + 1.2036e-12*x226 + 1.2092e-7*x227 + 4.9821e-7*x228 + 1.4959e-6*x229 + 0.00001469*x230 + 2.9567e-6*x231 + 4.3185e-6*x232 - 0.000011382*x233 + 0.000028375*x234 + 0.000036235*x235 - 1.1655e-6*x236 + 0.000048512*x237 - 2.556e-6*x238 - 0.00018108*x239 + 4.8965e-6*x240 + 0.0002473*x241 - 0.00046016*x242 - 1.5403e-7*x243 - 0.000035402*x244 - 6.692e-7*x245 - 0.000030872*x246 - 2.6423e-6*x247 - 0.000025093*x248 + 3.2698e-8*x249 + 0.000013066*x250 + 0.00027539*x251 + 7.0878e-6*x252 - 8.4539e-8*x253 + 6.5977e-6*x254 + 8.7116e-6*x255 + 1.2597e-7*x256 + 0.000018238*x257 - 6.2464e-8*x258 + 6.3177e-9*x259 + 2.0673e-8*x260 + 1.0956e-9*x261 - 4.7676e-8*x262 + 3.0357e-9*x263 - 5.5958e-8*x264 - 3.2116e-9*x265 + 4.7868e-8*x266 + 2.9561e-9*x267 - 5.0663e-10*x268 + 1.4761e-10*x269 + 0.000024774*x270 &y2 == 0.000053404*x136 - 2.2578e-7*x137 - 4.1785e-13*x138 - 6.2374e-6*x139 + 0.000012831*x140 - 1.9403e-7*x141 + 2.7938e-8*x142 - 1.2637e-6*x143 - 2.1746e-8*x144 + 4.8749e-6*x145 + 7.5165e-9*x146 + 4.4493e-6*x147 + 0.000036596*x148 - 6.6725e-8*x149 - 5.3733e-9*x150 + 1.1186e-6*x151 - 0.000022508*x152 - 1.5391e-9*x153 - 8.3956e-10*x154 - 3.6564e-7*x155 + 0.000016238*x156 - 8.6407e-9*x157 + 4.4821e-6*x158 - 0.000095172*x159 + 1.5596e-7*x160 + 0.0011788*x161 + 2.6296e-6*x162 - 4.5206e-6*x163 + 3.1541e-6*x164 + 3.4234e-6*x165 - 2.08e-8*x166 + 5.8236e-10*x167 - 3.5817e-8*x168 + 1.2169e-9*x169 - 1.6437e-8*x170 + 2.4764e-9*x171 + 5.7454e-9*x172 + 0.000041486*x173 - 1.1932e-7*x174 - 0.000017363*x175 - 1.9749e-7*x176 + 1.5264e-7*x177 - 6.2844e-6*x178 - 0.000019634*x179 - 0.000093417*x180 + 3.8909e-7*x181 - 1.1037e-6*x182 + 0.000019507*x183 + 0.00061185*x184 + 4.0791e-8*x185 + 0.00085036*x186 - 0.00077841*x187 + 1.1385e-8*x188 - 0.000026251*x189 - 0.000013098*x190 + 3.113e-6*x191 - 2.2957e-7*x192 + 1.5525e-6*x193 - 7.2933e-8*x194 - 1.6398e-8*x195 - 3.9396e-8*x196 + 1.1637e-7*x197 + 1.6346e-7*x198 - 2.122e-6*x199 + 6.7234e-8*x200 - 2.2165e-6*x201 - 4.0655e-7*x202 + 3.9448e-8*x203 + 5.5617e-8*x204 + 8.2242e-7*x205 + 5.717e-9*x206 - 2.0118e-8*x207 + 5.3465e-9*x208 - 2.898e-8*x209 - 4.3796e-10*x210 + 1.6929e-8*x211 + 2.3558e-6*x212 - 9.5461e-6*x213 - 0.000010895*x214 - 0.000011103*x215 + 3.1581e-7*x216 - 0.000025526*x217 - 2.0768e-7*x218 - 0.000028114*x219 + 1.4659e-6*x220 + 8.3431e-7*x221 + 4.7794e-7*x222 + 8.5727e-7*x223 + 1.2208e-6*x224 - 7.3512e-9*x225 - 8.1457e-13*x226 - 3.3339e-7*x227 + 8.7656e-8*x228 + 2.6222e-7*x229 + 5.0205e-7*x230 + 2.9824e-7*x231 + 3.2403e-8*x232 - 0.000016624*x233 - 5.1822e-6*x234 + 2.4175e-6*x235 + 6.1691e-6*x236 + 6.4016e-6*x237 + 0.00002134*x238 - 0.000020492*x239 - 0.000056018*x240 + 0.00073967*x241 + 0.00046546*x242 - 2.7161e-7*x243 + 1.7366e-6*x244 + 0.000018664*x245 - 4.57e-7*x246 + 6.1155e-6*x247 + 6.7812e-7*x248 - 8.041e-8*x249 - 3.15e-7*x250 + 1.1646e-6*x251 - 0.00010664*x252 + 3.85e-6*x253 + 1.308e-7*x254 + 0.000015031*x255 + 9.1582e-6*x256 - 6.8031e-9*x257 + 1.5913e-9*x258 + 7.8758e-9*x259 - 1.3071e-10*x260 + 3.7212e-9*x261 + 3.3024e-10*x262 + 9.9745e-9*x263 + 6.0178e-10*x264 - 8.8334e-9*x265 - 2.6781e-10*x266 + 1.1395e-8*x267 + 1.004e-10*x268 - 4.5906e-10*x269 - 1.3958e-7*x270 &y3 == 4.3036e-9*x136 - 0.000044272*x137 - 2.0608e-14*x138 - 1.519e-9*x139 - 2.5912e-9*x140 - 0.000038673*x141 + 0.000032498*x142 + 2.3747e-8*x143 - 0.000039651*x144 - 9.2512e-8*x145 + 0.000031103*x146 + 1.2869e-9*x147 + 7.1361e-9*x148 - 0.000020776*x149 - 9.3588e-7*x150 + 4.8503e-10*x151 + 3.0739e-8*x152 - 0.0000143*x153 + 5.664e-7*x154 + 1.2933e-9*x155 - 2.4537e-8*x156 + 5.8449e-6*x157 - 0.00071005*x158 - 4.4657e-7*x159 - 0.000042143*x160 + 4.5826e-7*x161 + 0.00060259*x162 - 1.1589e-7*x163 + 0.00075831*x164 + 1.9029e-8*x165 - 0.000017601*x166 + 9.0683e-8*x167 - 3.8217e-10*x168 - 3.9627e-8*x169 - 3.0249e-9*x170 - 1.0809e-8*x171 + 4.9123e-9*x172 + 9.5442e-8*x173 + 5.0547e-6*x174 + 1.7756e-7*x175 + 0.000038255*x176 - 0.000025123*x177 + 3.5648e-8*x178 - 8.8326e-7*x179 + 9.0962e-8*x180 - 7.5304e-6*x181 + 0.000085332*x182 - 1.4902e-7*x183 - 0.000018586*x184 + 0.000010995*x185 - 0.00089402*x186 - 0.00099197*x187 - 6.006e-6*x188 - 5.7554e-7*x189 - 1.2349e-7*x190 + 8.0273e-7*x191 + 1.6439e-6*x192 + 2.8894e-7*x193 - 1.2111e-7*x194 - 6.2718e-6*x195 - 4.6839e-8*x196 + 2.5022e-6*x197 + 0.000047033*x198 + 1.1044e-6*x199 - 4.7306e-6*x200 - 1.5125e-7*x201 + 1.3249e-8*x202 - 1.7176e-7*x203 + 5.504e-8*x204 - 1.8961e-8*x205 + 2.0255e-9*x206 + 1.0478e-10*x207 + 1.0975e-7*x208 + 9.0396e-9*x209 + 1.1095e-7*x210 - 3.3951e-10*x211 + 0.000033058*x212 - 3.7233e-7*x213 - 0.00016611*x214 + 3.7419e-7*x215 + 6.6528e-6*x216 + 6.1001e-7*x217 + 0.000016445*x218 + 2.3612e-6*x219 + 0.000020418*x220 + 6.9911e-7*x221 - 4.159e-7*x222 + 0.000023037*x223 - 3.9205e-7*x224 + 4.0622e-9*x225 + 3.2241e-13*x226 - 3.3394e-7*x227 - 1.6949e-7*x228 - 1.5104e-6*x229 - 0.000011329*x230 - 3.07e-6*x231 - 2.6338e-6*x232 + 0.000013308*x233 - 0.000029681*x234 - 0.000040243*x235 + 8.395e-7*x236 - 0.000060806*x237 + 1.7046e-6*x238 + 0.00022851*x239 - 2.3231e-6*x240 - 0.00041647*x241 + 0.00066465*x242 + 9.2145e-8*x243 + 0.000026844*x244 + 2.3841e-7*x245 + 0.000017667*x246 + 8.427e-7*x247 + 0.0001054*x248 - 1.0515e-8*x249 - 3.1863e-6*x250 - 7.3916e-6*x251 - 7.0902e-7*x252 + 3.959e-9*x253 + 0.000013865*x254 - 2.5121e-6*x255 - 5.9999e-8*x256 - 6.2216e-6*x257 - 1.7293e-8*x258 - 1.4806e-9*x259 + 5.7334e-9*x260 - 2.6634e-11*x261 - 1.3241e-8*x262 - 3.5952e-10*x263 - 1.6399e-8*x264 + 8.4377e-11*x265 + 1.4055e-8*x266 - 1.6175e-10*x267 - 1.4732e-10*x268 - 1.2063e-10*x269 - 0.000057812*x270";

		Expression e = FormulaParser.parseInvariant(s);
		Assert.assertNotNull(e);
	}

	@Test
	public void testTripleExpressionCondition()
	{
		String sampleGuard = "-5 <= x <= 5";

		Expression e = FormulaParser.parseGuard(sampleGuard);

		Assert.assertNotEquals(e, null);
	}

	@Test
	public void testScientificNotationCapitalE()
	{
		String sampleGuard = "-5 <= x <= 1.00E+03";

		Expression e = FormulaParser.parseGuard(sampleGuard);

		Assert.assertNotEquals(e, null);
	}

	@Test
	public void testPointNoZero()
	{
		String sampleGuard = "-.5 <= x <= .4e3";

		Expression e = FormulaParser.parseGuard(sampleGuard);

		Assert.assertNotEquals(e, null);
	}

	@Test
	public void testScientificNotation()
	{
		String sampleExpression = "0.5 >= x & x <= 0.123e4";

		Expression e = FormulaParser.parseGuard(sampleExpression);

		Assert.assertEquals("0.5 >= x & x <= 1230.0", DefaultExpressionPrinter.instance.print(e));

		sampleExpression = "1.5e-5 >= x & x <= 100.123e4";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("0.000015 >= x & x <= 1001230.0",
				DefaultExpressionPrinter.instance.print(e));

		sampleExpression = "-1.5e+5 >= x & x <= -100.123e-0";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("-150000.0 >= x & x <= -100.123",
				DefaultExpressionPrinter.instance.print(e));

		sampleExpression = "--1.5e0 >= x & x <= -100.123e+0";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("1.5 >= x & x <= -100.123", DefaultExpressionPrinter.instance.print(e));

		sampleExpression = "x == 9.1e16";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("x = 91000000000000000.0", DefaultExpressionPrinter.instance.print(e));

		sampleExpression = "x == 25.1e-16";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("x = 0.00000000000000251", DefaultExpressionPrinter.instance.print(e));

		// using 10^X
		sampleExpression = "x == 10^4.1";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("x = 10.0 ^ 4.1", DefaultExpressionPrinter.instance.print(e));
		// probably want to do this comparisons with regex to avoid spacing
		// complaints

		sampleExpression = "x == 10^-4.1";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("x = 10.0 ^ -4.1", DefaultExpressionPrinter.instance.print(e));

		sampleExpression = "x == 10^(0)";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("x = 10.0 ^ 0.0", DefaultExpressionPrinter.instance.print(e));

		sampleExpression = "x == 10^(-1)";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("x = 10.0 ^ -1.0", DefaultExpressionPrinter.instance.print(e));

		sampleExpression = "A == 7.89*10^-10.1"; // from E5/E5.xml example from
													// ODE/DAE test set
		e = FormulaParser.parseInitialForbidden(sampleExpression);
		Assert.assertEquals("A = 7.89 * 10.0 ^ -10.1", DefaultExpressionPrinter.instance.print(e));
	}

	@Test
	public void testTranscendentalFlow()
	{
		// this example flow is from the satellite model (Johnson et al, FM
		// 2012)
		String sampleFlow = "nu1' == sqrt( mu / (p1^3)) * ((1 + e1 * cos(nu1))^2)";

		Expression e = FormulaParser.parseFlow(sampleFlow);

		Assert.assertEquals("nu1 = sqrt(mu / p1 ^ 3.0) * (1.0 + e1 * cos(nu1)) ^ 2.0",
				DefaultExpressionPrinter.instance.print(e));
	}

	@Test
	public void testLocSingle()
	{
		String t = "loc(x) = y & x > 5";

		Expression e = FormulaParser.parseInitialForbidden(t);

		Assert.assertNotEquals(e, null);
	}

	@Test
	public void testLocNone()
	{
		String t = "x > 5";

		Expression e = FormulaParser.parseInitialForbidden(t);

		Assert.assertNotEquals(e, null);
	}

	@Test
	public void testLocExtract()
			throws EmptyRangeException, ConstantMismatchException, UnsupportedConditionException
	{
		String t = "x = boy & boy <= 5 & boy >= cat & cat = 5";

		Expression e = FormulaParser.parseInitialForbidden(t);

		RangeExtractor.getVariableRange(e, "x");
	}

	@Test
	public void testLocNested()
	{
		String t = "loc(automaton.x) = y & x > 5";

		Expression e = FormulaParser.parseInitialForbidden(t);

		Assert.assertNotEquals(e, null);
	}

	@Test
	public void testLocMultiple()
	{
		String t = "loc(automaton.x) = y & x > 5 & loc(automatonTwo.z) = jump";

		Expression e = FormulaParser.parseInitialForbidden(t);

		Assert.assertNotEquals(e, null);
	}

	@Test
	public void testLocDouble()
	{
		String t = "loc(x) = y & a > 5 | loc(x) = z & a > 6";

		try
		{
			FormulaParser.parseInitialForbidden(t);
		}
		catch (AutomatonExportException e)
		{
			Assert.fail("ors should be allowed in loc statements");
		}
	}

	@Test
	public void testLocEmptyComponentName()
	{
		String t = "x==0 & y==0 & loc()==one";

		FormulaParser.parseInitialForbidden(t);
	}

	@Test
	public void testParseFalseFlow()
	{
		Expression e = FormulaParser.parseFlow("false");

		if (e == null || !e.equals(Constant.FALSE))
			Assert.fail("flow not parsed to Constant.FALSE");
	}

	@Test
	public void testNestedGuard()
	{
		String t = "automaton.x <= 5";

		try
		{
			FormulaParser.parseGuard(t);

			Assert.fail("dotted variables are not allowed in guards");
		}
		catch (AutomatonExportException e)
		{
		}
	}

	@Test
	public void testNondetermResetTriple()
	{
		String t = "0 <= x' <= 0.1";

		Expression e = FormulaParser.parseReset(t);

		if (e == null || !DefaultExpressionPrinter.instance.print(e).equals("0.0 <= x & x <= 0.1"))
			// if (e == null ||
			// !DefaultExpressionPrinter.instance.print(e).equals("((0.0 <= x)
			// && (x <= 0.1))"))
			Assert.fail("parsed incorrectly, was: " + e.toDefaultString());
	}

	@Test
	public void testResetCombined()
	{
		String t = "1 <= z & y := x + 3 & 0 <= x' <= 0.1 & z <= 2";

		Expression e = FormulaParser.parseReset(t);

		if (e == null || !e.toDefaultString()
				.equals("1.0 <= z & y = x + 3.0 & 0.0 <= x & x <= 0.1 & z <= 2.0"))
			Assert.fail("parsed incorrectly, was: " + e.toDefaultString());
	}

	@Test
	public void testNondeterminisiticReset()
	{
		String sample = "T := 0 && ImaginaryChannel_min' >= 0 && ImaginaryChannel_min' <= 2147483647";

		Expression e = FormulaParser.parseReset(sample);

		Assert.assertNotEquals(e, null);

		Assert.assertEquals(
				"T = 0.0 & ImaginaryChannel_min >= 0.0 & ImaginaryChannel_min <= 2147483647.0",
				DefaultExpressionPrinter.instance.print(e));
	}

	/**
	 * Test the range extractor logic
	 */
	@Test
	public void testExtractRange()
	{
		Expression e = FormulaParser.parseGuard("y >= ---3"); // double
																// negatives
																// should get
																// cancelled in
																// parser

		// am.invariant = 1.0 <= range & range <= 2.0 & x <= 5.0

		try
		{
			TreeMap<String, Interval> vals = new TreeMap<String, Interval>();

			RangeExtractor.getVariableRanges(e, vals);
			Interval extracted = vals.get("y");

			if (extracted == null || !extracted.equals(new Interval(-3, Double.MAX_VALUE)))
				Assert.fail("Extracted range was not [-3, inf]. It was " + extracted);
		}
		catch (EmptyRangeException e1)
		{
			Assert.fail("range extractor raised exception.");
		}
		catch (ConstantMismatchException e1)
		{
			Assert.fail("range extractor raised constant mismatch exception.");
		}
		catch (UnsupportedConditionException condExp)
		{
			Assert.fail("range extractor raised unsupported condition exception.");
		}
	}

	/**
	 * Test the range extractor logic with a single variable
	 */
	@Test
	public void testExtractRangeSingle()
	{
		Expression e = FormulaParser.parseInvariant("1.0 <= range & range <= 2.0 & x <= 5.0");

		try
		{
			Interval extracted = RangeExtractor.getVariableRange(e, "range");

			if (extracted == null || !extracted.equals(new Interval(1, 2)))
				Assert.fail("Extracted range was not [1, 2]. It was " + extracted);
		}
		catch (EmptyRangeException e1)
		{
			Assert.fail("range extractor raised exception.");
		}
		catch (ConstantMismatchException e1)
		{
			Assert.fail("range extractor raised constant mismatch exception.");
		}
		catch (UnsupportedConditionException condExp)
		{
			Assert.fail("range extractor raised unsupported condition exception.");
		}
	}

	/**
	 * Test the range extractor logic for invalid range
	 */
	@Test
	public void testExtractRangeInvalid()
	{
		Expression e = FormulaParser.parseGuard("y >= 3 & y <= 2");

		try
		{
			TreeMap<String, Interval> vals = new TreeMap<String, Interval>();

			RangeExtractor.getVariableRanges(e, vals);

			Assert.fail(
					"range extractor should raise exception on invalid ranges. ranges returned = "
							+ vals);
		}
		catch (EmptyRangeException e1)
		{
		}
		catch (ConstantMismatchException e1)
		{
			Assert.fail("should be empty range");
		}
		catch (UnsupportedConditionException condExp)
		{
			Assert.fail("range extractor raised unsupported condition exception.");
		}
	}

	/**
	 * Test the range extractor logic for invalid range
	 */
	@Test
	public void testNullRange()
	{
		Expression e = FormulaParser.parseGuard("y >= 3 & y <= 2");

		try
		{
			Interval rv = RangeExtractor.getVariableRange(e, "x");

			if (rv != null)
				Assert.fail("range extractor should return null on empty range");
		}
		catch (EmptyRangeException e1)
		{
		}
		catch (ConstantMismatchException e1)
		{
			Assert.fail("should be empty range");
		}
		catch (UnsupportedConditionException condExp)
		{
			Assert.fail("range extractor raised unsupported condition exception.");
		}
	}

	/**
	 * Test the range extractor logic for invalid range
	 */
	@Test
	public void testConstantMismatch()
	{
		Expression e = FormulaParser.parseGuard("x = 2 && y >= 3 & y <= 4 & x == 4");

		try
		{
			RangeExtractor.getVariableRange(e, "y");

			Assert.fail("expected constant mismatch exception");
		}
		catch (EmptyRangeException e1)
		{
			Assert.fail("expected constant mismatch exception");
		}
		catch (ConstantMismatchException e1)
		{
			// expected
		}
		catch (UnsupportedConditionException condExp)
		{
			Assert.fail("range extractor raised unsupported condition exception.");
		}
	}

	@Test
	public void testExtractVarConstRange()
	{
		String sampleInv = "xmin == -5 & xmin <= x <= xmax & xmax == 6";

		Expression e = FormulaParser.parseInvariant(sampleInv);
		try
		{
			Interval range = RangeExtractor.getVariableRange(e, "x");

			if (range == null || !range.equals(new Interval(-5, 6)))
				Assert.fail("wrong range extracted: " + range);
		}
		catch (EmptyRangeException e1)
		{
			Assert.fail("empty range found");
		}
		catch (ConstantMismatchException e1)
		{
			Assert.fail("range extractor raised constant mismatch exception.");
		}
		catch (UnsupportedConditionException condExp)
		{
			Assert.fail("range extractor raised unsupported condition exception.");
		}
	}

	@Test
	public void testExtractVarConstRangeAlias()
			throws EmptyRangeException, ConstantMismatchException, UnsupportedConditionException
	{
		String sampleInv = "xmin == -5 & xmin <= x_alias <= xmax & xmax == 6 & x == x_alias";

		Expression e = FormulaParser.parseInvariant(sampleInv);

		Interval range = RangeExtractor.getVariableRange(e, "x");

		if (range == null || !range.equals(new Interval(-5, 6)))
			Assert.fail("wrong range extracted: " + range);

	}

	@Test
	public void testExtractRangeUnsupported()
	{
		String sampleInv = "x >= 0 && x <= 2 * 3.14";

		Expression e = FormulaParser.parseInvariant(sampleInv);
		try
		{
			Interval i = RangeExtractor.getVariableRange(e, "x");

			Assert.assertEquals(0, i.min, 1e-9);
			Assert.assertEquals(2 * 3.14, i.max, 1e-9);
		}
		catch (EmptyRangeException ex)
		{
			throw new RuntimeException("empty range found", ex);
		}
		catch (ConstantMismatchException e1)
		{
			Assert.fail("range extractor raised constant mismatch exception.");
		}
		catch (UnsupportedConditionException condExp)
		{
			Assert.fail("raised unsupported exception");
		}
	}

	@Test
	public void testExtractRangeUnsupported2()
	{
		String sampleInv = "x >= 0 && x <= 1 && x <= y";

		Expression e = FormulaParser.parseInvariant(sampleInv);
		try
		{
			System.out.println(RangeExtractor.getVariableRange(e, "x"));

			Assert.fail("didn't raise unsupported exception");
		}
		catch (EmptyRangeException ex)
		{
			throw new RuntimeException("empty range found", ex);
		}
		catch (ConstantMismatchException e1)
		{
			Assert.fail("range extractor raised constant mismatch exception.");
		}
		catch (UnsupportedConditionException condExp)
		{
			// expected
		}
	}

	@Test
	public void testEqualsInterval()
	{
		Expression[] expressions = { new Constant(0), new Constant(-5), new Variable("x") };
		Interval[] eq = { new Interval(0, 1), new Interval(-5, -4), null };
		Interval[] notEq = { new Interval(0, 2), new Interval(-4, -4), new Interval(0, 0) };

		for (int i = 0; i < expressions.length; ++i)
		{
			Expression e = expressions[i];
			Interval eqInt = eq[i];
			Interval neqInt = notEq[i];

			if (eqInt != null
					&& !new ExpressionInterval(e, new Interval(0, 1)).equalsInterval(eqInt))
				Assert.fail(e + " + [0, 1] != " + eqInt);

			if (new ExpressionInterval(e, new Interval(0, 1)).equalsInterval(neqInt))
				Assert.fail(e + " + [0, 1] == " + neqInt);
		}
	}

	/**
	 * Test using pow exponentials
	 */
	@Test
	public void testPow()
	{
		Expression e = FormulaParser.parseGuard("y >= 10^3");

		Assert.assertEquals("y >= 10.0 ^ 3.0", DefaultExpressionPrinter.instance.print(e));
	}

	/**
	 * Test using single number
	 */
	@Test
	public void testNumber()
	{
		Expression e = FormulaParser.parseValue("3.0");

		Expression simple = SimplifyExpressionsPass.simplifyExpression(e);

		if (!DefaultExpressionPrinter.instance.print(simple).equals("3.0"))
			Assert.fail("simplification failed: got: "
					+ DefaultExpressionPrinter.instance.print(simple));
	}

	/**
	 * Test using pow exponentials
	 */
	@Test
	public void testSimplifyPow()
	{
		Expression e = FormulaParser.parseValue("3 * 10^7");

		Expression simple = SimplifyExpressionsPass.simplifyExpression(e);

		Assert.assertEquals("30000000.0", DefaultExpressionPrinter.instance.print(simple));
	}

	@Test
	public void testMultiResetExpression()
	{
		String str = "g := fischer_agent_2_i & fischer_agent_2_x := 0.0 & g := 2.0";
		Expression exp = FormulaParser.parseReset(str);

		ArrayList<String> vars = new ArrayList<String>();
		vars.add("g");
		vars.add("fischer_agent_2_i");
		vars.add("fischer_agent_2_x");

		try
		{
			AutomatonUtil.extractExactAssignments(vars, exp);

			Assert.fail("multiple resets which could contradict not allowed");
		}
		catch (AutomatonExportException e)
		{
			// expected
		}
	}

	@Test
	public void testNonExactExtractReset()
	{
		Expression exp = FormulaParser.parseReset("x := x + y");

		ArrayList<String> vars = new ArrayList<String>();
		vars.add("x");
		vars.add("y");

		try
		{
			AutomatonUtil.extractReset(exp, vars);
		}
		catch (AutomatonExportException e)
		{
			Assert.fail("failed getting non-exact assignemnts");
		}
	}

	@Test
	public void testDReachExpressionPrinter()
	{
		DReachExpressionPrinter exp_printer = new DReachExpressionPrinter();

		String sampleGuard = "-5 <= x && x <= 5";
		Expression e1 = FormulaParser.parseGuard(sampleGuard);
		Assert.assertEquals("(and (-5.0 <= x) (x <= 5.0))", exp_printer.print(e1));

		String sampleFlow = "x' == x^x";
		Expression e3 = FormulaParser.parseFlow(sampleFlow);
		Assert.assertEquals("(x = x ^ x)", exp_printer.print(e3));

		sampleFlow = "x' == x * x";
		e3 = FormulaParser.parseFlow(sampleFlow);
		Assert.assertEquals("(x = x * x)", exp_printer.print(e3));

		sampleFlow = "x' == x^2";
		e3 = FormulaParser.parseFlow(sampleFlow);
		Assert.assertEquals("(x = x ^ 2.0)", exp_printer.print(e3));

		sampleFlow = "x' == x^2.1234";
		e3 = FormulaParser.parseFlow(sampleFlow);
		Assert.assertEquals("(x = x ^ 2.1234)", exp_printer.print(e3));
	}

	@Test
	public void testStateflowExpressionPrinterOne()
	{
		SimulinkStateflowPrinter spprinter = new SimulinkStateflowPrinter();
		SimulinkStateflowPrinter.SimulinkStateflowExpressionPrinter exp_printer = spprinter.new SimulinkStateflowExpressionPrinter(
				0);
		String sampleGuard = "-5 <= x <= 5";
		FormulaParser.parseGuard(sampleGuard);

		String sampleFlow = "nu1' = sqrt( mu / (p1^3)) * ((1 + e1 * cos(nu1))^2)";
		Expression e3 = FormulaParser.parseFlow(sampleFlow);
		Assert.assertEquals("nu1 = sqrt(mu / p1 ^ 3.0) * (1.0 + e1 * cos(nu1)) ^ 2.0",
				exp_printer.print(e3));

	}

	/**
	 * small tests to instance Hyst expressions from Matlab matrices
	 */
	@Test
	public void testSpaceExModelGeneration()
	{
		String flow = "x1 = 0.00751 * x1 - 5.275 * x2 + 0.0009639 * x3 - 0.6641 * x4 & x2 = 5.275 * x1 "
				+ "- 0.8575 * x2 + 0.09063 * x3 - 0.9218 * x4 & x3 = 0.0009639 * x1 - 0.09063 * x2 - 0.0001258 * x3 "
				+ "+ 13.54 * x4 & x4 = 0.6641 * x1 - 0.9218 * x2 - 13.54 * x3 - 1.004 * x4";

		Expression e1 = FormulaParser.parseFlow(flow);
		Assert.assertNotNull(e1);
		Assert.assertEquals(flow, DefaultExpressionPrinter.instance.print(e1));

		String inv = "y1 = 0.0006972 * x3 - 0.06453 * x2 - 0.006132 * x1 - 0.06223 * x4";
		Expression e2 = FormulaParser.parseInvariant(inv);
		Assert.assertNotNull(e2);
		Assert.assertEquals(inv, DefaultExpressionPrinter.instance.print(e2));

		BaseComponent ha = new BaseComponent();
		String[] strs = { "x1", "x2", "x3", "x4", "y1" };

		for (int i = 0; i < strs.length; i++)
		{
			ha.variables.add(strs[i]);
		}

		String name = "location1";
		ha.createMode(name, inv, flow);
	}

	@Test
	public void testLinearDetection()
	{
		String exp1 = "(1.0 - x * x) * y - x";

		Expression e1 = FormulaParser.parseValue(exp1);

		if (Classification.isLinearExpression(e1))
			Assert.fail("expression was detected as linear: " + exp1);

		String exp2 = "1.0 - x * 2 + 3 * y - z";

		Expression e2 = FormulaParser.parseValue(exp2);

		if (!(Classification.isLinearExpression(e2)))
			Assert.fail("expression was not detected as linear: " + exp2);
	}

	/*
	 * @Test public void testFlowstarLinearDetection() { String exp = "(1.0 - x * x) * y - x";
	 * 
	 * Expression e = FormulaParser.parseValue(exp);
	 * 
	 * if (FlowPrinter.isLinearExpression(e)) Assert.fail( "expression was detected as linear: " +
	 * exp); }
	 * 
	 * /** Make sure usage can be printed in Hyst (will also check for class loading issues)
	 */
	@Test
	public void testPrintUsage()
	{
		String[] args = { "-help" };

		Hyst.IS_UNIT_TEST = true;
		Assert.assertEquals(Hyst.runWithArguments(args), Hyst.ExitCode.SUCCESS.ordinal());
	}

	@Test
	public void testSubstituteExpression()
	{
		Expression e = FormulaParser.parseValue("5 * c + 2");
		Expression sub = new Operation(Operator.ADD, new Variable("c"),
				new IntervalTerm(new Interval(0, 1)));
		Expression result = AutomatonUtil.substituteVariable(e, "c", sub);

		Assert.assertTrue("Substituted in correctly",
				result.toDefaultString().equals("5.0 * (c + [0.0, 1.0]) + 2.0"));

		ExpressionInterval ei = ContinuizationPass.simplifyExpressionWithIntervals(result);

		Assert.assertTrue("substituted in interval was nonnull", ei.getInterval() != null);
		Assert.assertTrue("simplification resulted in correct interval",
				new Interval(0, 5).equals(ei.getInterval()));

		Assert.assertTrue("simplification resulted in correct expression",
				ei.getExpression().toDefaultString().equals("5.0 * c + 2.0"));
	}

	@Test
	public void testHarderExpression()
	{
		Expression e = FormulaParser.parseValue("-10 * v - 3 * a");
		Expression sub = new Operation(Operator.ADD, new Variable("a"),
				new IntervalTerm(new Interval(-1, 2)));
		Expression result = AutomatonUtil.substituteVariable(e, "a", sub);

		Assert.assertEquals("-10.0 * v - 3.0 * (a + [-1.0, 2.0])",
				DefaultExpressionPrinter.instance.print(result));

		ExpressionInterval ei = ContinuizationPass.simplifyExpressionWithIntervals(result);

		Assert.assertTrue("substituted in interval was nonnull", ei.getInterval() != null);

		Assert.assertTrue("simplification resulted in correct interval",
				new Interval(-6, 3).equals(ei.getInterval()));

		Expression expected = FormulaParser.parseValue("-10 * v - 3 * a");
		String errorMsg = AutomatonUtil.areExpressionsEqual(expected, ei.getExpression());

		if (errorMsg != null)
			Assert.fail(errorMsg);
	}

	@Test
	public void testFlowExpressionPrinter()
	{
		Expression.expressionPrinter = new FlowstarPrinter.FlowstarExpressionPrinter();
		Expression e1 = FormulaParser.parseInvariant("t <= 5");
		Expression e2 = FormulaParser.parseInvariant("5 <= t");
		Expression e3 = FormulaParser.parseInvariant("5 < t");

		Assert.assertEquals(e1.toString(), "t <= 5.0");
		Assert.assertEquals(e2.toString(), "5.0 - (t) <= 0");

		try
		{
			e3.toString();
			Assert.fail("Exception was not raised on strict inequality for flowstar");
		}
		catch (AutomatonExportException e)
		{
			// expected
		}
	}

	@Test
	public void testPrintNullExp()
	{
		Assert.assertEquals("null", DefaultExpressionPrinter.instance.print(null));
	}

	@Test
	public void testSimplifyCos()
	{
		Expression e = FormulaParser.parseValue("cos(t)");

		ExpressionInterval ei = ContinuizationPass.simplifyExpressionWithIntervalsRec(e);

		Assert.assertNotEquals("cos simplification is null", ei, null);
	}

	/**
	 * Tests the sampling-based jacobian estimation
	 */
	@Test
	public void testSampleJacobian()
	{

		LinkedHashMap<String, ExpressionInterval> dy = new LinkedHashMap<String, ExpressionInterval>();
		dy.put("x", new ExpressionInterval(FormulaParser.parseValue("2 * x + y")));
		dy.put("y", new ExpressionInterval(FormulaParser.parseValue("3 * y * x + y")));

		HashMap<String, Interval> bounds = new HashMap<String, Interval>();
		bounds.put("x", new Interval(1, 2));
		bounds.put("y", new Interval(2, 3));

		double[][] rv = AutomatonUtil.estimateJacobian(dy, bounds);

		// answer should be about:
		// 2.0 1.0
		// 7.5 5.5

		double TOL = 1e-6;
		Assert.assertEquals("Entry 0, 0 is correct", 2.0, rv[0][0], TOL);
		Assert.assertEquals("Entry 0, 1 is correct", 1.0, rv[0][1], TOL);
		Assert.assertEquals("Entry 1, 0 is correct", 7.5, rv[1][0], TOL);
		Assert.assertEquals("Entry 1, 1 is correct", 5.5, rv[1][1], TOL);
	}

	@Test
	/**
	 * Ensure a bind cannot have multiple parameters added with the same names (SpaceEx will not
	 * accept these if done and had an error where a model was created with multiple binds)
	 */
	public void testSpaceExNetworkComponentDuplicateBind()
	{
		// Network component
		SpaceExDocument sed = new SpaceExDocument();
		SpaceExNetworkComponent net = new SpaceExNetworkComponent(sed);
		Bind bind = new Bind(net);

		new ParamMap(bind, "key", "reference");

		try
		{
			new ParamMap(bind, "key", "reference");
			Assert.fail("inserting duplicate bind did not raise exception");
		}
		catch (AutomatonExportException e)
		{
			// expected
		}
	}

	private class ExpressionClassification
	{
		public String exp;
		public byte[] classes;

		public ExpressionClassification(String exp, byte... classification)
		{
			this.exp = exp;
			this.classes = classification;
		}
	}

	/**
	 * Test that functions get classified as expected
	 */
	@Test
	public void testClassifyOperators()
	{
		final ExpressionClassification[] tests = { new ExpressionClassification("x", (byte) 0),
				new ExpressionClassification("x + 1", AutomatonUtil.OPS_LINEAR),
				new ExpressionClassification("x^2", AutomatonUtil.OPS_NONLINEAR),
				new ExpressionClassification("sin(x)^2.5", AutomatonUtil.OPS_NONLINEAR),
				new ExpressionClassification("ln(x + 1)", AutomatonUtil.OPS_NONLINEAR,
						AutomatonUtil.OPS_LINEAR),
				new ExpressionClassification("lut([t], [0,0,1,1,0], [1,2,3,4,8])",
						AutomatonUtil.OPS_LUT, AutomatonUtil.OPS_MATRIX),
				new ExpressionClassification("-100 * x - 4 * v - 9.81",
						AutomatonUtil.OPS_LINEAR), };

		for (ExpressionClassification test : tests)
		{
			String expStr = test.exp;
			Expression e = FormulaParser.parseValue(expStr);

			boolean result = AutomatonUtil.expressionContainsOnlyAllowedOps(e, test.classes);
			Assert.assertTrue("Misclassified expression: '" + expStr + "'", result);
		}
	}

	/**
	 * Test that every operator in Hyst gets classified somewhere
	 */
	@Test
	public void testAllOperatorsHaveSomeClassification()
	{
		for (Operator o : Operator.values())
		{
			Operation op = new Operation(o);

			Assert.assertTrue(
					"Operator " + DefaultExpressionPrinter.instance.printOperator(o)
							+ " has no classification.",
					AutomatonUtil.classifyExpressionOps(op) != 0);
		}
	}

	@Test
	public void testDerivative()
	{
		Map<String, Expression> ders = new HashMap<String, Expression>();
		ders.put("x", FormulaParser.parseValue("x + y"));
		ders.put("y", FormulaParser.parseValue("1"));

		String[][] tests = { { "x", "x + y" }, { "y", "1" }, { "-y", "-1" }, { "- (-x)", "x + y" },
				{ "2 * x", "2 * (x + y)" }, { "x + 3 + y", "x + y + 1" }, { "1 - x", "-(x + y)" },
				{ "3 * x * y + const", "3 * (x + y) * y + 3 * x * 1" }, };

		for (String[] test : tests)
		{
			Expression original = FormulaParser.parseValue(test[0]);
			Expression result = AutomatonUtil.derivativeOf(original, ders);
			Expression expected = FormulaParser.parseValue(test[1]);

			String res = AutomatonUtil.areExpressionsEqual(expected, result);

			if (res != null)
				Assert.fail("Derivative of " + test[0] + " was wrong: " + res);
		}
	}

	@Test
	public void testParseDoubleNegative()
	{
		String t = "- (-x)";

		Expression e = FormulaParser.parseValue(t);

		Assert.assertNotEquals(e, null);
	}

	/*
	 * @Test public void testExtractDynamicsMatrixA() { String test = "-100 * x - 4 * v - 9.81";
	 * 
	 * Configuration c = AutomatonUtil .makeDebugConfiguration(new String[][] { { "x", "-v" }, {
	 * "v", test } });
	 * 
	 * AutomatonMode am = ((BaseComponent) c.root).modes.values().iterator().next();
	 * 
	 * ArrayList<ArrayList<Double>> matrix = DynamicsUtil.extractDynamicsMatrixA(am);
	 * 
	 * Assert.assertEquals(0.0, (double) matrix.get(0).get(0), 1e-9); Assert.assertEquals(-1.0,
	 * (double) matrix.get(0).get(1), 1e-9);
	 * 
	 * Assert.assertEquals(-100.0, (double) matrix.get(1).get(0), 1e-9); Assert.assertEquals(-4.0,
	 * (double) matrix.get(1).get(1), 1e-9); }
	 */
}
