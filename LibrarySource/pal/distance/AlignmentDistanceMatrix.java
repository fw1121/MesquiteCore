// AlignmentDistanceMatrix.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)

// Known bugs and limitations:
// - computational complexity of order O(numSeqs^2)


package pal.distance;

import pal.alignment.*;
import pal.substmodel.*;
import java.io.*;


/**
 * compute distance matrix (observed and ML) from alignment (SitePattern)
 *
 * @version $Id: AlignmentDistanceMatrix.java,v 1.6 2001/07/13 14:39:13 korbinian Exp $
 *
 * @author Korbinian Strimmer
 * @author Alexei Drummond
 */
public class AlignmentDistanceMatrix extends DistanceMatrix implements Serializable {
	//
	// Public stuff
	//

	/**
	 * compute observed distances
	 *
	 * @param sp site pattern
	 */
	public AlignmentDistanceMatrix(SitePattern sp)
	{
		numSeqs = sp.getSequenceCount();
		idGroup = sp;
		distance = new double[numSeqs][numSeqs];
		
		pwd = new PairwiseDistance(sp);

		computeDistances();
	}

	/**
	 * compute maximum-likelihood distances
	 *
	 * @param sp site pattern
	 * @param m  evolutionary model
	 */
	public AlignmentDistanceMatrix(SitePattern sp, SubstitutionModel m)
	{
		numSeqs = sp.getSequenceCount();
		idGroup = sp;
		distance = new double[numSeqs][numSeqs];
				
		pwd = new PairwiseDistance(sp, m);

		computeDistances();
	}

	/**
	 * recompute maximum-likelihood distances under new model
	 *
	 * @param m  evolutionary model
	 */
	public void recompute(SubstitutionModel m)
	{
		pwd.updateModel(m);

		computeDistances();
	}
	
	/**
	 * recompute maximum-likelihood distances under new site pattern
	 *
	 * @param sp site pattern
	 */
	public void recompute(SitePattern sp)
	{
		pwd.updateSitePattern(sp);

		computeDistances();
	}

	
	//
	// Private stuff
	//
	
	private PairwiseDistance pwd;
	
	private void computeDistances()
	{
		for (int i = 0; i < numSeqs; i++)
		{
			distance[i][i] = 0;
			for (int j = i + 1; j < numSeqs; j++)
			{
				distance[i][j] = pwd.getDistance(i, j);
				distance[j][i] = distance[i][j];
			}
		} 		
	}
}

