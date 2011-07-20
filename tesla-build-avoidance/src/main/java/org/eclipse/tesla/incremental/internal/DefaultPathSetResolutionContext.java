package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.PathSet;

class DefaultPathSetResolutionContext
    implements PathSetResolutionContext
{

    private final File outputDirectory;

    private final PathSet pathSet;

    private final boolean fullBuild;

    private final Selector selector;

    // input -> (timestamp, size)
    private final Map<File, FileState> inputStates;

    // output -> input
    private final Map<File, Set<File>> inputs;

    // input -> outputs
    private final Map<File, Collection<File>> outputs;

    public DefaultPathSetResolutionContext( BuildContext buildContext, PathSet pathSet, boolean fullBuild,
                                            Map<File, FileState> inputStates, Map<File, Set<File>> inputs,
                                            Map<File, Collection<File>> outputs )
    {
        this.outputDirectory = buildContext.getOutputDirectory();
        this.pathSet = pathSet;
        this.fullBuild = fullBuild;
        this.inputStates = inputStates;
        this.inputs = inputs;
        this.outputs = outputs;

        selector =
            new Selector( pathSet.getIncludes(), pathSet.getExcludes(), pathSet.isDefaultExcludes(),
                          pathSet.isCaseSensitive() );
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public boolean isFullBuild()
    {
        return fullBuild;
    }

    public PathSet getPathSet()
    {
        return pathSet;
    }

    public boolean isSelected( String pathname )
    {
        return selector.isSelected( pathname );
    }

    public boolean isAncestorOfPotentiallySelected( String pathname )
    {
        return selector.couldHoldIncluded( pathname );
    }

    public Collection<String> getDeletedInputPaths( Collection<File> existingInputs )
    {
        Collection<String> deletedInputPaths = new ArrayList<String>( 64 );

        File basedir = pathSet.getBasedir();
        for ( File file : inputStates.keySet() )
        {
            String pathname = FileUtils.relativize( file, basedir );
            if ( pathname != null && selector.isSelected( pathname ) && !existingInputs.contains( file ) )
            {
                deletedInputPaths.add( pathname );
            }
        }

        return deletedInputPaths;
    }

    public boolean isProcessingRequired( File input )
    {
        if ( fullBuild )
        {
            return true;
        }
        FileState previousState = inputStates.get( input );
        if ( previousState == null )
        {
            return true;
        }
        if ( previousState.getTimestamp() != input.lastModified() )
        {
            return true;
        }
        if ( previousState.getSize() != input.length() )
        {
            return true;
        }
        if ( isOutputMissing( input ) )
        {
            return true;
        }
        return false;
    }

    private boolean isOutputMissing( File input )
    {
        Collection<File> outputs = this.outputs.get( input );
        if ( outputs != null )
        {
            for ( File output : outputs )
            {
                if ( !output.exists() )
                {
                    return true;
                }
            }
        }
        return outputs == null || outputs.isEmpty();
    }

}
