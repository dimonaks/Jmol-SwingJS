/* $RCSfile$
 * $Author: aherraez $
 * $Date: 2009-01-15 21:00:00 +0100 (Thu, 15 Jan 2009) $
 * $Revision: 7752 $

 *
 * Copyright (C) 2003-2009  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.export;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.jmol.awtjs.swing.Font;
import javajs.util.A4;
import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.T3;

import javajs.util.BS;

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

/**
 * STereoLithography file format exporter
 * for 3D printing.
 *  
 * Based on _VrmlExporter by Bob Hanson;
 * diverts the output() method to do nothing and 
 * delivers only triangle sets.
 * 
 *  Note that no attempt is made here to ensure that
 *  surfaces are closed.
 *  
 *  Also, an inefficient mechanism that involves creating a 
 *  full in-memory representation of the data is used. If this
 *  turns out to be problematic, it might be possible to use
 *  random access, but the applet does not have that capability,
 *  and certainly JavaScript does not.
 *  
 */

public class _StlExporter extends _VrmlExporter {

  private boolean isDebug;
  private String header;
  private OC oc;
  private ByteArrayOutputStream bos;
  private M4 m4;

  public _StlExporter() {
    super();
    useTable = null;
    noColor = true;
    isDebug = Logger.debugging;
    if (!isDebug) {
      oc = new OC();
      oc.setBigEndian(false);
      oc.setParams(null, null, false, bos = new ByteArrayOutputStream());
    }
  }

  @Override
  protected void outputHeader() {
    //checkFile("c:/temp/t0.stl");
    header = ("solid model generated by Jmol " + Viewer.getJmolVersion() + "                                                                                ")
        .substring(0, 80);
    if (isDebug) {
      out.append(header);
      out.append("\n");
    } else {
      oc.write(header.getBytes(), 0, 80);
      oc.write(new byte[4], 0, 4);
    }
    lstMatrix = new Lst<M4>();
    m4 = new M4();
    m4.setIdentity();
    lstMatrix.addLast(m4);
    outputInitialTransform();
  }

  /*
  solid STL generated by MeshLab
  facet normal 1.861127e-001 -5.750880e-001 -7.966405e-001
    outer loop
      vertex  -4.747652e-002 2.344544e+000 -2.530380e-001
      vertex  -1.776902e-002 2.436463e+000 -3.124530e-001
      vertex  6.051898e-002 2.379494e+000 -2.530380e-001
    endloop
  endfacet
   */
  Lst<M4> lstMatrix;
  
  
  @Override
  protected void pushMatrix() {
    lstMatrix.addLast(m4);
    m4 = M4.newM4(m4);
  }

  @Override
   protected void popMatrix() {
    m4 = lstMatrix.removeItemAt(lstMatrix.size() - 1);
  }

  @Override
  protected void output(String data) {
    // not used!
  }

  @Override
  protected void outputChildStart() {
    // not used!
  }

  @Override
  protected void outputChildClose() {
    // not used!
  }

  private M4 m4a = new M4();

  @Override
  protected void outputRotation(A4 a) {
    m4a.setToAA(a);
    m4.mul(m4a);
  }

  @Override
  protected void outputAttrPt(String attr, T3 pt) {
    outputAttr(attr, pt.x, pt.y, pt.z);
  }

  @Override
  protected void outputAttr(String attr, float x, float y, float z) {
    m4a.setIdentity();
    if (attr == "scale") {
      m4a.m00 = x;
      m4a.m11 = y;
      m4a.m22 = z;
    } else if (attr == "translation") {
      m4a.m03 = x;
      m4a.m13 = y;
      m4a.m23 = z;
    }
    m4.mul(m4a);
  }
  
  private int nTri;

  @Override
  protected void outputGeometry(T3[] vertices, T3[] normals, short[] colixes,
                                int[][] indices, short[] polygonColixes,
                                int nVertices, int nPolygons,
                                BS bsPolygons, int faceVertexMax,
                                Lst<Short> colorList, Map<Short, Integer> htColixes, P3 offset) {
    for (int i = 0; i < nPolygons; i++) {
      if (bsPolygons != null && !bsPolygons.get(i))
        continue;
      int[] face = indices[i];
      writeFacet(vertices, face, 0, 1, 2);
      if (faceVertexMax == 4 && face.length >= 4 && face[2] != face[3])
        writeFacet(vertices, face, 2, 3, 0);       
    }

  }

  private void writeFacet(T3[] vertices, int[] face, int i, int j, int k) {
    tempQ1.setT(vertices[face[i]]);
    tempQ2.setT(vertices[face[j]]);
    tempQ3.setT(vertices[face[k]]);
    m4.rotTrans(tempQ1);
    m4.rotTrans(tempQ2);
    m4.rotTrans(tempQ3);
    Measure.calcNormalizedNormal(tempQ1, tempQ2, tempQ3, tempV1, tempV2);
    if (Float.isNaN(tempV1.x)) {
      return; // just a line -- can happen in cartoon meshes
    }
    writePoint("facet normal", tempV1);
    writePoint("outer loop\nvertex", tempQ1);
    writePoint("vertex", tempQ2);
    writePoint("vertex", tempQ3);
    if (isDebug) {
      out.append("endloop\nendfacet\n");
    } else {
      oc.writeByteAsInt(0);
      oc.writeByteAsInt(0);
    }
    nTri++;
  }

  @Override
  protected String finalizeOutput() {
    if (isDebug) {
      out.append("endsolid model\n");      
    } else {
      byte[] b = bos.toByteArray();
      b[80] = (byte) (nTri & 0xff);
      b[81] = (byte) ((nTri >> 8) & 0xff);
      b[82] = (byte) ((nTri >> 16) & 0xff);
      b[83] = (byte) ((nTri >> 24) & 0xff);
      out.write(b, 0, b.length);
    }    
    return finalizeOutput2();
  }

  @Override
  protected void outputCircle(P3 pt1, P3 pt2, float radius, short colix,
                            boolean doFill) {
    // not implemented for solids
  }
  
  @Override
  void plotText(int x, int y, int z, short colix, String text, Font font3d) {
    // not implemented for solids
  }

  ///////////////  raw ASCII/binary write methods
  
  private void writePoint(String s, T3 p) {
    if (isDebug)
      out.append(s);
    writeFloat(p.x);
    writeFloat(p.y);
    writeFloat(p.z);
    if (isDebug)
      out.append("\n");
  }

  private void writeFloat(float f) {
    if (isDebug)
      out.append(" " + f);
    else
      oc.writeInt(Float.floatToIntBits(f));
  }

//@SuppressWarnings("unused")
//private void checkFile(String fname) {
//  BinaryDocument doc = new BinaryDocument();
//  doc.setStream((BufferedInputStream) vwr.fm
//      .getBufferedReaderOrErrorMessageFromName(fname, null, true, false),
//      false);
//  try {
//    for (int j = 0; j < 20; j++)
//      doc.readInt();
//    System.out.println(doc.readInt());
//    for (int j = 0; j < 10; j++) {
//      for (int k = 0; k < 12; k++) {
//        float f = doc.readFloat();
//        System.out.println(j + " " + f + "\t"
//            + Integer.toHexString(Float.floatToIntBits(f)));
//        if (k % 3 == 2)
//          System.out.println("");
//      }
//      System.out.println(j + " " + doc.readShort() + "\n");
//    }
//  } catch (Exception e) {
//    // ignore
//  }
//  doc.close();
//}


}
